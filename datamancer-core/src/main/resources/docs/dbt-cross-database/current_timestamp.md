---
title: "current_timestamp"
sidebar_label: "current_timestamp"
id: "current_timestamp"
description: "This macro returns the current date and time for the system."
---

This macro returns the current date and time for the system. Depending on the adapter:

* The result may be an aware or naive timestamp.
* The result may correspond to the start of the statement or the start of the transaction.

**Usage**:

```sql
{{ dbt.current_timestamp() }}
```

**Sample Output (PostgreSQL)**:

```sql
now()
```
