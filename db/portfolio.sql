CREATE TABLE event(
    event_id INT NOT NULL AUTO_INCREMENT,
    ext_id INT,
    source VARCHAR(16),
    trade_date DATETIME,
    event_type VARCHAR(16),
    instrument VARCHAR(32),
    quantity INT,
    amount DECIMAL(10,2),
    price DECIMAL(10,5),
    currency CHAR(3),
    cur_rate DECIMAL(10,5),
    profit DECIMAL(10,2),
    PRIMARY KEY (event_id)
    UNIQUE KEY (event_id, source)
) ENGINE=InnoDB;

CREATE TABLE quote(
    instrument VARCHAR(32),
    date DATETIME,
    open DECIMAL(10,5),
    high DECIMAL(10,5),
    low DECIMAL(10,5),
    close DECIMAL(10,5),
    volume INT
)