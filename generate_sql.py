
def generate_sql(prefix, tenant):
    categories = {
        "FT": ("Fuel Truck", "Fueling"),
        "CT": ("Catering Truck", "Catering"),
        "BT": ("Baggage Tug", "Ground Support"),
        "BC": ("Baggage Cart", "Ground Support"),
        "BL": ("Belt Loader", "Cargo"),
        "GP": ("GPU", "Power"),
        "PB": ("Pushback", "Ground Support"),
        "ST": ("Stairs", "Ground Support"),
        "WT": ("Water Truck", "Services"),
        "LV": ("Lavatory Truck", "Services"),
        "DI": ("De-icing Truck", "Services"),
        "AS": ("ASU", "Ground Support"),
        "BU": ("Bus", "Transport"),
        "CG": ("Cargo Loader", "Cargo"),
        "AM": ("Ambulift", "Services")
    }
    
    counts = {
        "FT": 10, "CT": 8, "BT": 12, "BC": 15, "BL": 8, "GP": 6, "PB": 8, "ST": 6,
        "WT": 4, "LV": 4, "DI": 3, "AS": 4, "BU": 8, "CG": 3, "AM": 1
    }
    
    sql = []
    for code, (name, cat) in categories.items():
        count = counts[code]
        for i in range(1, count + 1):
            asset_id = f"{prefix}-{code}-{i:02d}"
            asset_name = f"{name} {i} ({tenant})"
            sql.append(f"INSERT INTO assets (id, asset_id, name, qr_id, status, category, tenant_code) VALUES (uuid_generate_v4(), '{asset_id}', '{asset_name}', '{asset_id}', 'Active', '{cat}', '{tenant}') ON CONFLICT DO NOTHING;")
    
    return sql

vidp_sql = generate_sql("DEL", "VIDP")
ybbn_sql = generate_sql("BNE", "YBBN")

with open("insert_assets.sql", "w") as f:
    f.write("-- VIDP Assets\n")
    f.writelines(s + "\n" for s in vidp_sql)
    f.write("\n-- YBBN Assets\n")
    f.writelines(s + "\n" for s in ybbn_sql)

print("SQL script generated: insert_assets.sql")
