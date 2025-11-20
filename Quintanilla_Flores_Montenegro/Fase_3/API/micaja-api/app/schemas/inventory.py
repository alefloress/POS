from pydantic import BaseModel
from typing import Optional
from datetime import date


class ProductBase(BaseModel):
    code: str
    name: str
    sale_price: float
    cost_price: Optional[float] = None
    stock: int = 0
    reorder_threshold: int = 0
    expiry_date: Optional[date] = None
    description: Optional[str] = None


class ProductCreate(ProductBase):
    pass


class ProductUpdate(BaseModel):
    name: Optional[str] = None
    sale_price: Optional[float] = None
    cost_price: Optional[float] = None
    stock: Optional[int] = None
    reorder_threshold: Optional[int] = None
    expiry_date: Optional[date] = None
    description: Optional[str] = None
    is_active: Optional[bool] = None


class ProductOut(ProductBase):
    id: int
    is_active: bool

    class Config:
        from_attributes = True


class SupplierBase(BaseModel):
    tax_id: str | None = None
    name: str
    contact_name: str | None = None
    email: str | None = None
    phone: str | None = None
    address: str | None = None
    notes: str | None = None


class SupplierCreate(SupplierBase):
    pass


class SupplierOut(SupplierBase):
    id: int
    is_active: bool

    class Config:
        from_attributes = True
