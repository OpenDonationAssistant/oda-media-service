  alter table video add provider varchar(255);
  update video set provider = 'youtube' where 1=1;
