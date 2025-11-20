# app/routers/sysadmin.py

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.database import get_session
from app.deps import require_sysadmin
from app.security import hash_pwd
from app.schemas.users import AdminCreateIn, AdminOut, TenantCreateIn, AdminWithStoreOut

router = APIRouter(
    prefix="/sysadmin",
    tags=["sysadmin"],
)

# ----------- LISTAR ADMINS (sólo SYSADMIN) -----------
@router.get(
    "/admins",
    response_model=list[AdminOut],
    dependencies=[Depends(require_sysadmin)],
)
async def list_admins(db: AsyncSession = Depends(get_session)):
    q = text(
        """
        SELECT id, username, full_name, role, is_active
        FROM users
        WHERE role = 'admin'
        ORDER BY id
        """
    )
    rows = (await db.execute(q)).mappings().all()

    # La BD guarda "admin" en minúsculas, pero el esquema AdminOut
    # exige Literal["ADMIN"], así que normalizamos aquí.
    return [
        AdminOut(
            id=r["id"],
            username=r["username"],
            full_name=r["full_name"],
            role="ADMIN",
            is_active=r["is_active"],
        )
        for r in rows
    ]


# ----------- CREAR ADMIN (sólo SYSADMIN) -----------
@router.post(
    "/admins",
    response_model=AdminOut,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_sysadmin)],
)
async def create_admin(
    data: AdminCreateIn,
    db: AsyncSession = Depends(get_session),
):
    # 1) ¿Ya existe el username?
    q_check = text("""
        SELECT 1
        FROM users
        WHERE username = :u
        LIMIT 1
    """)
    exists = (await db.execute(q_check, {"u": data.username})).scalar()
    if exists:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="USUARIO_YA_EXISTE",
        )

    # 2) Crear tienda nueva para este admin
    #    Puedes cambiar estos textos luego en un panel de edición.
    full_name = data.full_name or data.username
    store_name = f"Tienda de {full_name}"

    q_store = text("""
        INSERT INTO stores (plan_id, name, contact_name, contact_email, is_active)
        VALUES (:plan_id, :name, :contact_name, :contact_email, TRUE)
        RETURNING id
    """)

    store_row = (
        await db.execute(
            q_store,
            {
                "plan_id": 1,  # plan por defecto
                "name": store_name,
                "contact_name": full_name,
                # correo dummy por ahora; luego tendrás pantalla para editarlo
                "contact_email": f"{data.username}@pending.local",
            },
        )
    ).mappings().first()

    store_id = store_row["id"]

    # 3) Crear el usuario admin asociado a esa tienda
    q_insert = text("""
        INSERT INTO users
            (store_id, username, full_name, role, password_hash, is_active)
        VALUES
            (:store_id, :username, :full_name, 'admin', :password_hash, :is_active)
        RETURNING id, username, full_name, role, is_active
    """)

    params = {
        "store_id": store_id,
        "username": data.username,
        "full_name": full_name,
        "password_hash": hash_pwd(data.password),
        "is_active": data.is_active,
    }

    row = (await db.execute(q_insert, params)).mappings().first()
    await db.commit()

    return AdminOut(
        id=row["id"],
        username=row["username"],
        full_name=row["full_name"],
        role="ADMIN",
        is_active=row["is_active"],
    )



# ----------- ELIMINAR ADMIN (sólo SYSADMIN) -----------
@router.delete(
    "/admins/{admin_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_sysadmin)],
)
async def delete_admin(
    admin_id: int,
    db: AsyncSession = Depends(get_session),
):
    q_delete = text(
        """
        DELETE FROM users
        WHERE id = :id
          AND role = 'admin'
        RETURNING id
        """
    )
    row = (await db.execute(q_delete, {"id": admin_id})).first()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="ADMIN_NO_ENCONTRADO",
        )

    await db.commit()
    # 204 → sin body
    
@router.post(
    "/tenants",
    response_model=AdminWithStoreOut,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_sysadmin)],
)
async def create_tenant(
    data: TenantCreateIn,
    db: AsyncSession = Depends(get_session),
):
    # 1) Validar que no exista username
    q_check_user = text("""
        SELECT 1
        FROM users
        WHERE username = :u
        LIMIT 1
    """)
    exists_user = (await db.execute(q_check_user, {"u": data.admin_username})).scalar()
    if exists_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="ADMIN_USERNAME_YA_EXISTE",
        )

    # 2) Insertar tienda
    q_store = text("""
        INSERT INTO stores
            (plan_id, name, contact_name, contact_email, is_active)
        VALUES
            (:plan_id, :name, :contact_name, :contact_email, TRUE)
        RETURNING id, name
    """)

    store_row = (
        await db.execute(
            q_store,
            {
                "plan_id": data.plan_id,
                "name": data.store_name,
                "contact_name": data.contact_name,
                "contact_email": data.contact_email,
            },
        )
    ).mappings().first()

    store_id = store_row["id"]
    store_name = store_row["name"]

    # 3) Insertar admin principal para esa tienda
    full_name = data.admin_full_name or data.admin_username

    q_admin = text("""
        INSERT INTO users
            (store_id, username, full_name, role, password_hash, is_active)
        VALUES
            (:store_id, :username, :full_name, 'admin', :password_hash, :is_active)
        RETURNING id, username, full_name, role, is_active
    """)

    admin_row = (
        await db.execute(
            q_admin,
            {
                "store_id": store_id,
                "username": data.admin_username,
                "full_name": full_name,
                "password_hash": hash_pwd(data.admin_password),
                "is_active": data.admin_is_active,
            },
        )
    ).mappings().first()

    await db.commit()

    return AdminWithStoreOut(
        id=admin_row["id"],
        username=admin_row["username"],
        full_name=admin_row["full_name"],
        role="ADMIN",                     # normalizado
        is_active=admin_row["is_active"],
        store_id=store_id,
        store_name=store_name,
    )
