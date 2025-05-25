create table settings (
  id varchar(255),
  recipient_id varchar(255),
  song_request_cost integer,
  max_amount integer,
  request_view_amount integer,
  requests_enabled boolean,
  youtube_enabled boolean,
  vkvideo_enabled boolean,
  request_tooltip text,
  wordsBlacklist text
)
