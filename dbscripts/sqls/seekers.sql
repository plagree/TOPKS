insert into seekers 
SELECT "user", 100 
from soc_tag_80
group by "user", item
having count(item)=
(select max(itemcount)
from (select "user", count(*) as itemcount
from soc_tag_80
group by "user", item)tab)
limit 1;


