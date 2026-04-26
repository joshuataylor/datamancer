---
title: "accepted_range"
sidebar_label: "accepted_range"
id: "accepted_range"
description: "Asserts that a column's values fall inside an expected range. Any combination of `min_value` and `max_value` is allowed, and the range can be inclusive or exclusive. Provide [a `where` argument](ht..."
category: "Generic Tests"
---

Asserts that a column's values fall inside an expected range. Any combination of `min_value` and `max_value` is allowed, and the range can be inclusive or exclusive. Provide [a `where` argument](https://docs.getdbt.com/reference/resource-configs/where) to filter to specific records only.

In addition to comparisons to a scalar value, you can also compare to another column's values. Any data type that supports the `>` or `<` operators can be compared, so you could also run tests like checking that all order dates are in the past.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: user_id
        tests:
          - dbt_utils.accepted_range:
              arguments:
                min_value: 0
                inclusive: false

      - name: account_created_at
        tests:
          - dbt_utils.accepted_range:
              arguments:
                max_value: "getdate()"
                #inclusive is true by default

      - name: num_returned_orders
        tests:
          - dbt_utils.accepted_range:
              arguments:
                min_value: 0
                max_value: "num_orders"

      - name: num_web_sessions
        tests:
          - dbt_utils.accepted_range:
              arguments:
                min_value: 0
                inclusive: false
              config:
                where: "num_orders > 0"
```
