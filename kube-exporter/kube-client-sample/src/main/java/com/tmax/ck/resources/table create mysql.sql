CREATE TABLE ci_instance_history (
	ci_history_id	VARCHAR(512)	NOT NULL,
	time	CHAR(10)	NOT NULL,
	ci_type	VARCHAR(255)	NOT NULL,
	json	JSON	NOT NULL
);

ALTER TABLE ci_instance_history ADD CONSTRAINT PK_CI_INSTANCE_HISTORY PRIMARY KEY (
	ci_history_id
);