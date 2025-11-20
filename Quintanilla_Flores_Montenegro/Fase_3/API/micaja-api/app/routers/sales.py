from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.database import get_session
from app.deps import get_current
from app.schemas.sales import SaleCreate, SaleOut, SaleItemIn

router = APIRouter(prefix="/sales", tags=["sales"])

IVA = 0.19

@router.post("", response_model=SaleOut, status_code=status.HTTP_201_CREATED)
async def create_sale(
    payload: SaleCreate,
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]
    user_id = claims["uid"]

    # 1) Validar sesión de caja
    q_sess = text(
        """
        SELECT cs.id
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
            q_sess,
            {"sid": payload.session_id, "uid": user_id, "store_id": store_id},
        )
    ).first()
    if not sess:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="SESION_CAJA_INVALIDA",
        )

    # 2) Calcular totales
    subtotal = sum(i.quantity * i.unit_price for i in payload.items)
    tax = round(subtotal * IVA, 2)
    total = round(subtotal + tax, 2)

 # 2.1) Normalizar y validar método de pago
    method = payload.payment_method.lower()

    valid_methods = {"cash", "card", "transfer", "other"}  # ajusta a lo que tengas en la BD
    if method not in valid_methods:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="METODO_PAGO_INVALIDO",
        )
    
    # 3) Insertar cabecera de venta
    q_sale = text(
        """
        INSERT INTO sales
            (store_id, session_id, user_id,
             created_at, subtotal, tax, total,
             payment_method, status,
             customer_name, customer_tax_id)
        VALUES
            (:store_id, :session_id, :user_id,
             NOW(), :subtotal, :tax, :total,
             :payment_method, :status,
             :customer_name, :customer_tax_id)
        RETURNING
            id, created_at, subtotal, tax, total,
            payment_method, status, user_id
        """
    )
    sale_row = (
        await db.execute(
            q_sale,
            {
                "store_id": store_id,
                "session_id": payload.session_id,
                "user_id": user_id,
                "subtotal": subtotal,
                "tax": tax,
                "total": total,
                "payment_method": method,  # <--- aquí
                "status": payload.status,
                "customer_name": payload.customer_name,
                "customer_tax_id": payload.customer_tax_id,
            },
        )
    ).mappings().first()

    sale_id = sale_row["id"]

    # 4) Insertar items + actualizar stock
    q_item = text(
        """
        INSERT INTO sale_items
            (sale_id, product_id, quantity, unit_price, tax, total)
        VALUES
            (:sale_id, :product_id, :qty, :price, :tax, :total)
        """
    )
    q_stock = text(
        """
        UPDATE products
        SET stock = stock - :qty
        WHERE id = :product_id
          AND store_id = :store_id
        """
    )

    for item in payload.items:
        line_subtotal = item.quantity * item.unit_price
        line_tax = round(line_subtotal * IVA, 2)
        line_total = round(line_subtotal + line_tax, 2)

        await db.execute(
            q_item,
            {
                "sale_id": sale_id,
                "product_id": item.product_id,
                "qty": item.quantity,
                "price": item.unit_price,
                "tax": line_tax,
                "total": line_total,
            },
        )

        await db.execute(
            q_stock,
            {
                "product_id": item.product_id,
                "qty": item.quantity,
                "store_id": store_id,
            },
        )

    await db.commit()
    return SaleOut(**sale_row)


@router.get("/{sale_id}", response_model=SaleOut)
async def get_sale(
    sale_id: int,
    claims=Depends(get_current),
    db: AsyncSession = Depends(get_session),
):
    store_id = claims["store_id"]

    q = text(
        """
        SELECT
            id, created_at, subtotal, tax, total,
            payment_method, status, user_id
        FROM sales
        WHERE id = :sale_id
          AND store_id = :store_id
        """
    )
    row = (
        await db.execute(q, {"sale_id": sale_id, "store_id": store_id})
    ).mappings().first()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="VENTA_NO_ENCONTRADA",
        )

    return SaleOut(**row)