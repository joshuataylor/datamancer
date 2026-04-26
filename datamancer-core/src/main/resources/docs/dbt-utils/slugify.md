---
title: "slugify"
sidebar_label: "slugify"
id: "slugify"
description: "This macro is useful for transforming Jinja strings into \"slugs\", and can be useful when using a Jinja object as a column name, especially when that Jinja object is not hardcoded."
category: "Jinja Helpers"
---

This macro is useful for transforming Jinja strings into "slugs", and can be useful when using a Jinja object as a column name, especially when that Jinja object is not hardcoded.

For this example, let's pretend that we have payment methods in our payments table like `['venmo App', 'ca$h-money', '1337pay']`, which we can't use as a column name due to the spaces and special characters. This macro does its best to strip those out in a sensible way: `['venmo_app',
'cah_money', '_1337pay']`.

```sql
{%- set payment_methods = dbt_utils.get_column_values(
    table=ref('raw_payments'),
    column='payment_method'
) -%}

select
order_id,
{%- for payment_method in payment_methods %}
sum(case when payment_method = '{{ payment_method }}' then amount end)
  as {{ dbt_utils.slugify(payment_method) }}_amount,

{% endfor %}
...
```

```sql
select
order_id,

sum(case when payment_method = 'Venmo App' then amount end)
  as venmo_app_amount,

sum(case when payment_method = 'ca$h money' then amount end)
  as cah_money_amount,

sum(case when payment_method = '1337pay' then amount end)
  as _1337pay_amount,
...
```
---
