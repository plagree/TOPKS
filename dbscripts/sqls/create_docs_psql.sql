insert INTO docs (select item, tag, count(*) as num from tagging group by tag, item);
