INSERT INTO tagfreq (select tag, count(*) as num from tagging group by tag);
