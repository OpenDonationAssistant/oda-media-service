delete from video a using video b where a.ctid < b.ctid and a.id = b.id;
delete from video where id is null;

alter table video add constraint video_pkey primary key (id);
