---
title: "get_single_value"
sidebar_label: "get_single_value"
id: "get_single_value"
description: "This macro returns a single value from a sql query, so that you don't need to interact with the Agate library to operate on the result"
category: "Introspective macros"
---

This macro returns a single value from a sql query, so that you don't need to interact with the Agate library to operate on the result

**Usage:**

```sql
{% set sql_statement %}
    select max(created_at) from {{ ref('processed_orders') }}
{% endset %}

{%- set newest_processed_order = dbt_utils.get_single_value(sql_statement, default="'2020-01-01'") -%}

select

    *,
    last_order_at > '{{ newest_processed_order }}' as has_unprocessed_order

from {{ ref('users') }}
```
