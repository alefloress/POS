from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class SaleItemIn(BaseModel):
    product_id: int
    quantity: int
    unit_price: float

class SaleCreate(BaseModel):
    session_id: int
    payment_method: str           # p.ej. "CASH", "CARD"
    status: str                   # p.ej. "SALE"
    customer_name: Optional[str] = None
    customer_tax_id: Optional[str] = None
    items: List[SaleItemIn]

class SaleOut(BaseModel):
    id: int
    created_at: datetime
    subtotal: float
    tax: float
    total: float
    payment_method: str
    status: str
    user_id: int

    class Config:
        from_attributes = True