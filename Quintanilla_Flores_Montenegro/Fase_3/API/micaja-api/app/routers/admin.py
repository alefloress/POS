from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.database import get_session
from app.deps import require_admin
from app.security import hash_pwd
from app.schemas.users import UserCreate, UserOut, UserUpdateIn

router = APIRouter(
    prefix="/admin",
    tags=["admin"],
)

@router.get("/users")
async def list_users(
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    q = text("""
        SELECT id, username, full_name, role, is_active
        FROM users
        WHERE store_id = :store_id
          AND role = 'cashier'   -- SOLO cajeros
        ORDER BY id
    """)

    rows = (await db.execute(q, {"store_id": store_id})).mappings().all()

    items = [
        UserOut(
            id=r["id"],
            username=r["username"],
            full_name=r["full_name"],
            role=r["role"],
            is_active=r["is_active"],
        )
        for r in rows
    ]

    return {"items": items}

@router.post(
    "/users",
    response_model=UserOut,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin)],
)
async def create_user(
    data: UserCreate,
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]   # <--- AQUÍ se hereda

    # comprobar username único por tienda
    q_check = text("""
        SELECT 1
        FROM users
        WHERE username = :u
          AND store_id = :store_id
        LIMIT 1
    """)
    exists = (
        await db.execute(q_check, {"u": data.username, "store_id": store_id})
    ).scalar()
    if exists:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="USUARIO_YA_EXISTE",
        )

    q_insert = text("""
        INSERT INTO users
            (store_id, username, full_name, role, password_hash, is_active)
        VALUES
            (:store_id, :username, :full_name, :role, :password_hash, :is_active)
        RETURNING id, username, full_name, role, is_active
    """)

    params = {
        "store_id": store_id,                     # <--- heredado del admin
        "username": data.username,
        "full_name": data.full_name,
        "role": data.role,                        # normalmente "cashier"
        "password_hash": hash_pwd(data.password),
        "is_active": data.is_active,
    }

    row = (await db.execute(q_insert, params)).mappings().first()
    await db.commit()

    return UserOut(
        id=row["id"],
        username=row["username"],
        full_name=row["full_name"],
        role=row["role"],
        is_active=row["is_active"],
    )


async def create_user(
    data: UserCreate,
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # El admin sólo puede crear usuarios en su tienda
    store_id = claims["store_id"]

    # 1) ¿Ya existe el username en esa tienda?
    q_check = text(
        """
        SELECT 1
        FROM users
        WHERE username = :u
          AND store_id = :store_id
        LIMIT 1
        """
    )
    exists = (
        await db.execute(q_check, {"u": data.username, "store_id": store_id})
    ).scalar()

    if exists:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="USUARIO_YA_EXISTE",
        )

    # 2) Insertar usuario
    q_insert = text(
        """
        INSERT INTO users
            (store_id, username, full_name, role, password_hash, is_active)
        VALUES
            (:store_id, :username, :full_name, :role, :password_hash, :is_active)
        RETURNING id, username, full_name, role, is_active
        """
    )

    params = {
        "store_id": store_id,
        "username": data.username,
        "full_name": data.full_name,
        "role": data.role,
        "password_hash": hash_pwd(data.password),
        "is_active": data.is_active,
    }

    row = (await db.execute(q_insert, params)).mappings().first()
    await db.commit()

    return UserOut(
        id=row["id"],
        username=row["username"],
        full_name=row["full_name"],
        role=row["role"],
        is_active=row["is_active"],
    )

@router.get("/cash-registers")
async def list_cash_registers(
    claims = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # TODO: SELECT cash_registers WHERE store_id = claims["store_id"]
    return {"items": []}


@router.post("/cash-registers")
async def create_cash_register(
    claims = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # TODO: insertar caja respetando límites de plan
    return {"status": "pending"}

@router.put(
    "/users/{user_id}",
    response_model=UserOut,
    dependencies=[Depends(require_admin)],
)
async def update_user(
    user_id: int,
    data: UserUpdateIn,
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    # 1) Traer datos actuales del usuario (solo cajeros de su tienda)
    q_cur = text("""
        SELECT id, username, full_name, role, is_active, password_hash
        FROM users
        WHERE id = :id
          AND store_id = :store_id
          AND role = 'cashier'
        LIMIT 1
    """)

    cur = (await db.execute(q_cur, {"id": user_id, "store_id": store_id})).mappings().first()

    if not cur:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="USUARIO_NO_ENCONTRADO",
        )

    # 2) Resolver nuevos valores (si no vienen, se mantienen)
    new_full_name = data.full_name or cur["full_name"]
    new_role      = data.role or cur["role"]          # si quieres, puedes forzar 'cashier'
    new_is_active = cur["is_active"] if data.is_active is None else data.is_active

    new_password_hash = cur["password_hash"]
    if data.password is not None:
        new_password_hash = hash_pwd(data.password)

    # 3) UPDATE
    q_upd = text("""
        UPDATE users
        SET full_name     = :full_name,
            role          = :role,
            is_active     = :is_active,
            password_hash = :password_hash
        WHERE id       = :id
          AND store_id = :store_id
          AND role     = 'cashier'
        RETURNING id, username, full_name, role, is_active
    """)

    params = {
        "id": user_id,
        "store_id": store_id,
        "full_name": new_full_name,
        "role": new_role,
        "is_active": new_is_active,
        "password_hash": new_password_hash,
    }

    row = (await db.execute(q_upd, params)).mappings().first()
    await db.commit()

    return UserOut(
        id=row["id"],
        username=row["username"],
        full_name=row["full_name"],
        role=row["role"],
        is_active=row["is_active"],
    )

async def update_user(
    user_id: int,
    data: UserUpdateIn,
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # Solo puede tocar usuarios de su misma tienda
    store_id = claims["store_id"]

    # 1) Traer datos actuales del usuario
    q_cur = text(
        """
        SELECT id, username, full_name, role, is_active, password_hash
        FROM users
        WHERE id = :id
          AND store_id = :store_id
        LIMIT 1
        """
    )
    cur = (await db.execute(q_cur, {"id": user_id, "store_id": store_id})).mappings().first()

    if not cur:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="USUARIO_NO_ENCONTRADO",
        )

    # 2) Resolver nuevos valores (si vienen, se actualizan; si no, se mantienen)
    new_full_name = data.full_name or cur["full_name"]
    new_role      = data.role or cur["role"]
    new_is_active = cur["is_active"] if data.is_active is None else data.is_active

    new_password_hash = cur["password_hash"]
    if data.password is not None:
        new_password_hash = hash_pwd(data.password)

    # 3) UPDATE en BD
    q_upd = text(
        """
        UPDATE users
        SET full_name     = :full_name,
            role          = :role,
            is_active     = :is_active,
            password_hash = :password_hash
        WHERE id       = :id
          AND store_id = :store_id
        RETURNING id, username, full_name, role, is_active
        """
    )

    params = {
        "id": user_id,
        "store_id": store_id,
        "full_name": new_full_name,
        "role": new_role,
        "is_active": new_is_active,
        "password_hash": new_password_hash,
    }

    row = (await db.execute(q_upd, params)).mappings().first()
    await db.commit()

    return UserOut(
        id=row["id"],
        username=row["username"],
        full_name=row["full_name"],
        role=row["role"],
        is_active=row["is_active"],
    )

@router.delete(
    "/users/{user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_admin)],
)
async def delete_user(
    user_id: int,
    claims: dict = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    # Opcional: evitar que borre otros admins
    q_del = text(
        """
        DELETE FROM users
        WHERE id = :id
          AND store_id = :store_id
          AND role <> 'admin'      -- evita borrar admins, solo cajeros/usuarios
        RETURNING id
        """
    )

    row = (await db.execute(q_del, {"id": user_id, "store_id": store_id})).first()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="USUARIO_NO_ENCONTRADO",
        )

    await db.commit()
    # 204 → sin body
