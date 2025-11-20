# app/routers/inventory.py
from fastapi import APIRouter, Depends, Query, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.database import get_session
from app.deps import get_current, require_admin
from app.schemas.inventory import (
    ProductOut,
    ProductCreate,
    ProductUpdate,
    SupplierOut,
)

router = APIRouter(prefix="/inventory", tags=["inventory"])


# --------- LISTAR PRODUCTOS (con filtro opcional q) -----------
@router.get("/products", response_model=list[ProductOut])
async def list_products(
    q: str | None = Query(default=None),
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    sql = """
        SELECT
            id,
            code,
            name,
            description,
            sale_price,
            cost_price,
            stock,
            reorder_threshold,
            expiry_date,
            is_active
        FROM products
        WHERE store_id = :store_id
    """

    params: dict = {"store_id": store_id}

    if q:
        sql += """
           AND (
                code ILIKE :pattern
             OR name ILIKE :pattern
             OR COALESCE(description, '') ILIKE :pattern
           )
        """
        params["pattern"] = f"%{q}%"

    sql += " ORDER BY id"

    rows = (await db.execute(text(sql), params)).mappings().all()

    return [ProductOut(**row) for row in rows]


# --------- OBTENER 1 PRODUCTO POR ID --------------------------
@router.get("/products/{product_id}", response_model=ProductOut)
async def get_product(
    product_id: int,
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    q = text(
        """
        SELECT
            id,
            code,
            name,
            description,
            sale_price,
            cost_price,
            stock,
            reorder_threshold,
            expiry_date,
            is_active
        FROM products
        WHERE id = :id
          AND store_id = :store_id
        LIMIT 1
        """
    )

    row = (
        await db.execute(q, {"id": product_id, "store_id": store_id})
    ).mappings().first()

    if not row:
        raise HTTPException(status_code=404, detail="PRODUCTO_NO_ENCONTRADO")

    return ProductOut(**row)


# --------- CREAR PRODUCTO (sólo ADMIN) ------------------------
@router.post(
    "/products",
    response_model=ProductOut,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin)],
)
async def create_product(
    data: ProductCreate,
    claims=Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    # (Opcional) Verificar que no exista mismo code en la tienda
    q_check = text(
        """
        SELECT 1
        FROM products
        WHERE store_id = :store_id
          AND code = :code
        LIMIT 1
        """
    )
    exists = (
        await db.execute(q_check, {"store_id": store_id, "code": data.code})
    ).scalar()

    if exists:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="CODIGO_YA_EXISTE",
        )

    q_ins = text(
        """
        INSERT INTO products
            (store_id, code, name, description,
             sale_price, cost_price, stock,
             reorder_threshold, expiry_date, is_active)
        VALUES
            (:store_id, :code, :name, :description,
             :sale_price, :cost_price, :stock,
             :reorder_threshold, :expiry_date, TRUE)
        RETURNING
            id, code, name, description,
            sale_price, cost_price, stock,
            reorder_threshold, expiry_date, is_active
        """
    )

    params = {
        "store_id": store_id,
        "code": data.code,
        "name": data.name,
        "description": data.description,
        "sale_price": data.sale_price,
        "cost_price": data.cost_price,
        "stock": data.stock,
        "reorder_threshold": data.reorder_threshold,
        "expiry_date": data.expiry_date,
    }

    row = (await db.execute(q_ins, params)).mappings().first()
    await db.commit()

    return ProductOut(**row)


# --------- ACTUALIZAR PRODUCTO (sólo ADMIN) -------------------
@router.put(
    "/products/{product_id}",
    response_model=ProductOut,
    dependencies=[Depends(require_admin)],
)
async def update_product(
    product_id: int,
    data: ProductUpdate,
    claims=Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    # Traer datos actuales
    q_cur = text(
        """
        SELECT
            id,
            code,
            name,
            description,
            sale_price,
            cost_price,
            stock,
            reorder_threshold,
            expiry_date,
            is_active
        FROM products
        WHERE id = :id
          AND store_id = :store_id
        LIMIT 1
        """
    )
    cur = (
        await db.execute(q_cur, {"id": product_id, "store_id": store_id})
    ).mappings().first()

    if not cur:
        raise HTTPException(status_code=404, detail="PRODUCTO_NO_ENCONTRADO")

    # Resolver nuevos valores
    new_values = {
        "name": data.name if data.name is not None else cur["name"],
        "description": (
            data.description if data.description is not None else cur["description"]
        ),
        "sale_price": (
            data.sale_price if data.sale_price is not None else cur["sale_price"]
        ),
        "cost_price": (
            data.cost_price if data.cost_price is not None else cur["cost_price"]
        ),
        "stock": data.stock if data.stock is not None else cur["stock"],
        "reorder_threshold": (
            data.reorder_threshold
            if data.reorder_threshold is not None
            else cur["reorder_threshold"]
        ),
        "expiry_date": (
            data.expiry_date if data.expiry_date is not None else cur["expiry_date"]
        ),
        "is_active": (
            cur["is_active"] if data.is_active is None else data.is_active
        ),
    }

    q_upd = text(
        """
        UPDATE products
        SET
            name = :name,
            description = :description,
            sale_price = :sale_price,
            cost_price = :cost_price,
            stock = :stock,
            reorder_threshold = :reorder_threshold,
            expiry_date = :expiry_date,
            is_active = :is_active
        WHERE id = :id
          AND store_id = :store_id
        RETURNING
            id, code, name, description,
            sale_price, cost_price, stock,
            reorder_threshold, expiry_date, is_active
        """
    )

    params = {
        "id": product_id,
        "store_id": store_id,
        **new_values,
    }

    row = (await db.execute(q_upd, params)).mappings().first()
    await db.commit()

    return ProductOut(**row)


# --------- ELIMINAR PRODUCTO (soft delete: is_active = false) -
@router.delete(
    "/products/{product_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(require_admin)],
)
async def delete_product(
    product_id: int,
    claims=Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    q_del = text(
        """
        UPDATE products
        SET is_active = FALSE
        WHERE id = :id
          AND store_id = :store_id
          AND is_active = TRUE
        RETURNING id
        """
    )

    row = (
        await db.execute(q_del, {"id": product_id, "store_id": store_id})
    ).first()

    if not row:
        raise HTTPException(status_code=404, detail="PRODUCTO_NO_ENCONTRADO")

    await db.commit()
    # 204 → sin body


# --------- LISTAR PROVEEDORES (lo dejamos stub) ---------------
@router.get("/suppliers", response_model=list[SupplierOut])
async def list_suppliers(
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    # TODO: implementar si lo necesitas
    return []
