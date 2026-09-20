import random
import string

from sqlalchemy.orm import Session

from app.models.customer import Customer


def generate_customer_code(db: Session) -> str:
    """NS-CUST-000123-style code. Not guaranteed sequential across
    concurrent inserts (no DB sequence exists for it in schema.sql); collision
    is avoided by retrying on the unique constraint."""
    while True:
        count = db.query(Customer).count()
        candidate = f"NS-CUST-{count + 1:06d}"
        exists = db.query(Customer.id).filter(Customer.customer_code == candidate).first()
        if exists is None:
            return candidate
        # extremely unlikely race; fall back to a random suffix
        candidate = f"NS-CUST-{count + 1:06d}{random.choice(string.ascii_uppercase)}"
        if db.query(Customer.id).filter(Customer.customer_code == candidate).first() is None:
            return candidate
