from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.database import get_session
from app.schemas.auth import LoginIn, TokenPair, UserInfo
from app.security import verify_pwd, make_access_token
from app.deps import get_current

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=TokenPair)
async def login(payload: LoginIn, db: AsyncSession = Depends(get_session)):
    q = text("""
    SELECT
        u.id,
        u.username,
        u.full_name,
        u.role,
        u.password_hash,
        s.id        AS store_id,
        s.name      AS store_name,
        s.is_active AS store_active,
        u.is_active
    FROM users u
    JOIN stores s ON s.id = u.store_id
    WHERE u.username = :u
    LIMIT 1
    """)
    row = (await db.execute(q, {"u": payload.username})).mappings().first()

    if not row:
        raise HTTPException(status_code=401, detail="CREDENCIALES_INVALIDAS")

    if not row["is_active"] or not row["store_active"]:
        raise HTTPException(status_code=403, detail="USUARIO_O_TIENDA_INACTIVA")

    if not verify_pwd(payload.password, row["password_hash"]):
        raise HTTPException(status_code=401, detail="CREDENCIALES_INVALIDAS")

    # SYSADMIN vs ADMIN
    if row["username"] == "sysadmin":
        role_claim = "SYSADMIN"
    else:
        role_claim = "ADMIN"   # o row["role"].upper()

    token = make_access_token(
        sub=row["username"],
        uid=row["id"],
        role=role_claim,
        store_id=row["store_id"],
    )

    # IMPORTANTE: devolver SIEMPRE JSON, NO el token solo
    return {
        "access_token": token,
        "token_type": "bearer",
    }


@router.get("/whoami", response_model=UserInfo)
async def whoami(
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    q = text("""
        SELECT u.full_name, s.name AS store_name
        FROM users u
        JOIN stores s ON s.id = u.store_id
        WHERE u.id = :uid
    """)
    row = (await db.execute(q, {"uid": claims["uid"]})).mappings().first()

    return UserInfo(
        id=claims["uid"],
        username=claims["sub"],
        full_name=row["full_name"] if row else "",
        role=claims["role"],
        store_id=claims["store_id"],
        store_name=row["store_name"] if row else "",
    )
