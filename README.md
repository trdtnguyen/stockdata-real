# stockdata
Update stock information (daily prices, volume) for common markets e..g, S&amp;P500, NASDAQ100

## How it works
The program first scan a list of stock symbols. For each symbol, it request the server for fetching stock data. We've provided the list of symbols for SP500 and NASDAQ100 at the time the program is published. You could modify the list as your need.

## Requirements
* **List of stock symbols**: The program scan the list of stock symbols as the input. You could customize the list.
* **Libraries**: `commons-codec-1.6`,  `commons-logging-1.1.3`, `fluent-hc-4.3`, `httpclient-4.3`, `httpclient-cache-4.3`, `httpcore-4.3`, `httpmime-4.3`, `javacsv`.

## How to run the program
1. Edit `config.cnf`. Adjust `FROM_DATE` and `TO_DATE` for your desired period. Change value of   `SYMBOL_LIST_FILE` for SP500 or NASDAQ100.

2. Run the jar file:

`java -jar stockdata.jar`

3. Output: the list of stock prices as text file in `OUT_DIR` specified in `config.cnf` file.
