import datetime

def calculate_status(opening_time="09:00", closing_time="22:00", now_dt=None):
    if now_dt is None:
        now_dt = datetime.datetime.now(datetime.timezone.utc)
    
    current_time = now_dt.time()
    current_date = now_dt.date()
    
    open_h, open_m = map(int, opening_time.split(':'))
    close_h, close_m = map(int, closing_time.split(':'))
    
    open_time = datetime.time(open_h, open_m)
    close_time = datetime.time(close_h, close_m)
    
    is_overnight = close_time <= open_time
    
    if not is_overnight:
        # Normal Business (e.g., 09:00 -> 22:00)
        if open_time <= current_time < close_time:
            is_open = True
            business_date = current_date
        else:
            is_open = False
            if current_time < open_time:
                business_date = current_date - datetime.timedelta(days=1)
            else:
                business_date = current_date
    else:
        # Overnight Business (e.g., 18:00 -> 03:00)
        if current_time >= open_time:
            is_open = True
            business_date = current_date
        elif current_time < close_time:
            is_open = True
            business_date = current_date - datetime.timedelta(days=1)
        else:
            is_open = False
            business_date = current_date - datetime.timedelta(days=1)
            
    return {
        "isOpen": is_open,
        "isClosed": not is_open,
        "businessDate": str(business_date),
        "currentTime": now_dt.strftime("%Y-%m-%dT%H:%M:%S")
    }

print("=== MILESTONE 2: BUSINESS CLOCK MATRIX VERIFICATION ===")

tz = datetime.timezone.utc
base_date = datetime.date(2026, 8, 5)

matrix = [
    # Normal Business (09:00 -> 22:00)
    ("09:00", "22:00", datetime.time(8, 59), False, "2026-08-04"),
    ("09:00", "22:00", datetime.time(9, 0), True, "2026-08-05"),
    ("09:00", "22:00", datetime.time(12, 0), True, "2026-08-05"),
    ("09:00", "22:00", datetime.time(21, 59), True, "2026-08-05"),
    ("09:00", "22:00", datetime.time(22, 0), False, "2026-08-05"),
    
    # Overnight Business (18:00 -> 03:00)
    ("18:00", "03:00", datetime.time(17, 59), False, "2026-08-04"),
    ("18:00", "03:00", datetime.time(18, 0), True, "2026-08-05"),
    ("18:00", "03:00", datetime.time(22, 0), True, "2026-08-05"),
    ("18:00", "03:00", datetime.time(2, 59), True, "2026-08-04"),
    ("18:00", "03:00", datetime.time(3, 0), False, "2026-08-04"),
]

passed = 0
failed = 0

for open_t, close_t, test_t, exp_open, exp_date in matrix:
    dt = datetime.datetime.combine(base_date, test_t, tzinfo=tz)
    res = calculate_status(open_t, close_t, dt)
    
    status_match = res["isOpen"] == exp_open
    date_match = res["businessDate"] == exp_date
    
    if status_match and date_match:
        print(f"[PASS] {open_t}-{close_t} @ {test_t.strftime('%H:%M')} -> Open:{res['isOpen']}, Date:{res['businessDate']}")
        passed += 1
    else:
        print(f"[FAIL] {open_t}-{close_t} @ {test_t.strftime('%H:%M')} -> Got Open:{res['isOpen']}, Date:{res['businessDate']} | Expected Open:{exp_open}, Date:{exp_date}")
        failed += 1

print(f"\nRESULTS: {passed} PASSED, {failed} FAILED out of {len(matrix)} tests.")
