---
title: "get_query_results_as_dict"
sidebar_label: "get_query_results_as_dict"
id: "get_query_results_as_dict"
description: "This macro returns a dictionary from a sql query, so that you don't need to interact with the Agate library to operate on the result"
category: "Introspective macros"
---

This macro returns a dictionary from a sql query, so that you don't need to interact with the Agate library to operate on the result

**Usage:**

```sql
{% set sql_statement %}
    select city, state from {{ ref('users') }}
{% endset %}

{%- set places = dbt_utils.get_query_results_as_dict(sql_statement) -%}

select

    {% for city in places['CITY'] | unique -%}
      sum(case when city = '{{ city }}' then 1 else 0 end) as users_in_{{ dbt_utils.slugify(city) }},
    {% endfor %}

    {% for state in places['STATE'] | unique -%}
      sum(case when state = '{{ state }}' then 1 else 0 end) as users_in_{{ state }},
    {% endfor %}

    count(*) as total_total

from {{ ref('users') }}
```
