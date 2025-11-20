from enum import Enum
from pydantic import BaseModel, EmailStr, StringConstraints
from typing import Optional, Literal, Annotated

Username = Annotated[str, StringConstraints(min_length=3, max_length=50)]
Password = Annotated[str, StringConstraints(min_length=6, max_length=128)]
FullName = Annotated[str, StringConstraints(min_length=1, max_length=100)]


# -------------------------------------------------
# Roles permitidos en la tabla users
# -------------------------------------------------
class UserRole(str, Enum):
    ADMIN = "admin"
    CASHIER = "cashier"
    SYSADMIN = "sysadmin"   # ← NUEVO rol


class TenantCreateIn(BaseModel):
    # Datos de la tienda
    store_name: str
    plan_id: int = 1
    contact_name: str
    contact_email: EmailStr

    # Datos del admin principal
    admin_username: Username
    admin_password: Password
    admin_full_name: Optional[FullName] = None
    admin_is_active: bool = True

class AdminCreateIn(BaseModel):
    username: Username
    password: Password
    # Nombre opcional, si no viene usamos el username
    full_name: Optional[FullName] = None
    is_active: bool = True
    
class AdminOut(BaseModel):
    id: int
    username: str
    full_name: str | None = None
    role: Literal["ADMIN"]
    is_active: bool

    class Config:
        from_attributes = True


class UserBase(BaseModel):
    username: str
    full_name: str
    # 'admin' | 'cashier' | 'sysadmin'
    role: UserRole


class UserUpdateIn(BaseModel):
    full_name: Optional[str] = None
    role: Optional[UserRole] = None        # normalmente "cashier" o "admin"
    password: Optional[str] = None        # si viene, se cambia
    is_active: Optional[bool] = None
    
    
class UserCreate(UserBase):
    password: str
    is_active: bool = True


class UserOut(UserBase):
    id: int
    is_active: bool

    class Config:
        from_attributes = True


class StoreBase(BaseModel):
    name: str
    contact_name: str
    contact_email: EmailStr


class StoreOut(StoreBase):
    id: int
    plan_id: int

    class Config:
        from_attributes = True
        

class SysadminLoginIn(BaseModel):
    username: str
    password: str


class AdminWithStoreOut(AdminOut):
    store_id: int
    store_name: str
    
class TenantOut(BaseModel):
    store_id: int
    store_name: str
    admin_id: int
    admin_username: str

    class Config:
        from_attributes = True
