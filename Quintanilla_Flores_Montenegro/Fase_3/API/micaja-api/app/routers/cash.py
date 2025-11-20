from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.database import get_session
from app.deps import get_current
from app.schemas.cash import CashOpenIn, CashCloseIn, CashSessionOut

router = APIRouter(prefix="/cash", tags=["cash"])


@router.post("/open", response_model=CashSessionOut)
async def open_cash(
    payload: CashOpenIn,
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]
    user_id = claims["uid"]

    # 1) Verificar que la caja existe y pertenece a la tienda del token
    q_reg = text(
        """
        SELECT id
        FROM cash_registers
        WHERE id = :rid
          AND store_id = :store_id
          AND is_active = true
        """
    )
    reg = (await db.execute(q_reg, {"rid": payload.register_id, "store_id": store_id})).first()
    if not reg:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="CAJA_NO_ENCONTRADA",
        )

    # 2) Verificar que no haya ya una sesión abierta en esa caja
    q_open = text(
        """
        SELECT id
        FROM cash_sessions
        WHERE register_id = :rid
          AND status = 'OPEN'
        LIMIT 1
        """
    )
    already = (await db.execute(q_open, {"rid": payload.register_id})).first()
    if already:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="CAJA_YA_ABIERTA",
        )

    # 3) Insertar sesión nueva
    q_ins = text(
        """
        INSERT INTO cash_sessions
            (register_id, user_id, opened_at, opening_amount, status)
        VALUES
            (:rid, :uid, NOW(), :opening, 'OPEN')
        RETURNING
            id, register_id, user_id, opened_at, closed_at,
            opening_amount, declared_amount, closing_amount,
            status, note
        """
    )
    row = (
        await db.execute(
            q_ins,
            {"rid": payload.register_id, "uid": user_id, "opening": payload.opening_amount},
        )
    ).mappings().first()

    await db.commit()
    return CashSessionOut(**row)

@router.post("/close", response_model=CashSessionOut)
async def close_cash(
    payload: CashCloseIn,
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]
    user_id = claims["uid"]

    # Verificar que la sesión pertenece a la tienda y está abierta
    q_sel = text(
        """
        SELECT
            cs.id
        FROM cash_sessions cs
        JOIN cash_registers cr ON cr.id = cs.register_id
        WHERE cs.id = :sid
          AND cs.status = 'OPEN'
          AND cs.user_id = :uid
          AND cr.store_id = :store_id
        """
    )
    sess = (
        await db.execute(
            q_sel,
            {"sid": payload.session_id, "uid": user_id, "store_id": store_id},
        )
    ).first()
    if not sess:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="SESION_CAJA_NO_VALIDA",
        )

    q_upd = text(
        """
        UPDATE cash_sessions
        SET
            closed_at       = NOW(),
            declared_amount = :declared,
            closing_amount  = :closing,
            status          = 'CLOSED',
            note            = :note
        WHERE id = :sid
        RETURNING
            id, register_id, user_id,
            opened_at, closed_at,
            opening_amount, declared_amount,
            closing_amount, status, note
        """
    )
    row = (
        await db.execute(
            q_upd,
            {
                "sid": payload.session_id,
                "declared": payload.declared_amount,
                "closing": payload.closing_amount,
                "note": payload.note,
            },
        )
    ).mappings().first()

    await db.commit()
    return CashSessionOut(**row)

@router.get("/active", response_model=CashSessionOut)
async def get_active_cash(
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]
    user_id = claims["uid"]

    q = text(
        """
        SELECT
            cs.id, cs.register_id, cs.user_id,
            cs.opened_at, cs.closed_at,
            cs.opening_amount, cs.declared_amount,
            cs.closing_amount, cs.status, cs.note
        FROM cash_sessions cs
        JOIN cash_registers cr ON cr.id = cs.register_id
        WHERE cr.store_id = :store_id
          AND cs.user_id = :uid
          AND cs.status = 'OPEN'
        ORDER BY cs.opened_at DESC
        LIMIT 1
        """
    )
    row = (await db.execute(q, {"store_id": store_id, "uid": user_id})).mappings().first()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="SIN_CAJA_ABIERTA",
        )

    return CashSessionOut(**row)