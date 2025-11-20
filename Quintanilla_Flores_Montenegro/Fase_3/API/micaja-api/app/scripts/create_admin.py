# app/scripts/create_admin.py

import asyncio
from getpass import getpass

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import AsyncSessionLocal
from app.security import hash_pwd


async def main() -> None:
    print("=== Crear/actualizar usuario ADMIN de prueba ===")

    username = input("Username [admin]: ") or "admin"
    full_name = input("Nombre completo [Admin Prueba]: ") or "Admin Prueba"
    role = "admin"

    # pides contraseña sin eco
    password = getpass("Password [admin123 por defecto]: ") or "admin123"

    pwd_hash = hash_pwd(password)

    async with AsyncSessionLocal() as db:  
        store_row = (
            await db.execute(text("SELECT id FROM stores LIMIT 1"))
        ).first()

        if store_row is None:
            plan_row = (
                await db.execute(text("SELECT id FROM plans LIMIT 1"))
            ).first()

            if plan_row is None:
                # plan mínimo
                plan_id = (
                    await db.execute(
                        text(
                            """
                            INSERT INTO plans (name, max_cashiers, max_registers, price_monthly, is_active)
                            VALUES ('TESIS', 10, 10, 0, TRUE)
                            RETURNING id
                            """
                        )
                    )
                ).scalar_one()
            else:
                plan_id = plan_row[0]

            store_id = (
                await db.execute(
                    text(
                        """
                        INSERT INTO stores (plan_id, name, contact_name, contact_email, is_active)
                        VALUES (:plan_id, 'Almacen Prueba', 'Admin Prueba', 'admin@prueba.cl', TRUE)
                        RETURNING id
                        """
                    ),
                    {"plan_id": plan_id},
                )
            ).scalar_one()
            print(f"Creada tienda por defecto con id={store_id}")
        else:
            store_id = store_row[0]
            print(f"Usando tienda existente id={store_id}")

        # upsert muy simple por username
        existing = (
            await db.execute(
                text("SELECT id FROM users WHERE username = :u"), {"u": username}
            )
        ).first()

        if existing:
            await db.execute(
                text(
                    """
                    UPDATE users
                    SET full_name = :full_name,
                        role = :role,
                        password_hash = :pwd_hash,
                        is_active = TRUE
                    WHERE id = :id
                    """
                ),
                {
                    "full_name": full_name,
                    "role": role,
                    "pwd_hash": pwd_hash,
                    "id": existing[0],
                },
            )
            print(f"Usuario '{username}' actualizado (id={existing[0]}).")
        else:
            new_id = (
                await db.execute(
                    text(
                        """
                        INSERT INTO users (store_id, username, full_name, role, password_hash, is_active)
                        VALUES (:store_id, :username, :full_name, :role, :pwd_hash, TRUE)
                        RETURNING id
                        """
                    ),
                    {
                        "store_id": store_id,
                        "username": username,
                        "full_name": full_name,
                        "role": role,
                        "pwd_hash": pwd_hash,
                    },
                )
            ).scalar_one()
            print(f"Usuario '{username}' creado con id={new_id}.")

        await db.commit()


if __name__ == "__main__":
    asyncio.run(main())
