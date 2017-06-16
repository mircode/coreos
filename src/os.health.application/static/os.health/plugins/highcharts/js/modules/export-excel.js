/**
 * A small plugin for getting the CSV of a categorized chart
 */
(function (Highcharts) {
    

    var each = Highcharts.each;
     		   
    Highcharts.Chart.prototype.getData = function () {
    	var columns = [],
    	options = (this.options.exporting || {}).csv || {},
    	dateFormat = options.dateFormat || '%Y-%m-%d';
    	
    	each (this.series, function (series) {
            if (series.options.includeInCSVExport !== false && series.name!="Navigator") {
                if (series.xAxis) {
                    var xData = series.xData.slice(),
                        xTitle = 'X values';
                    if (series.xAxis.isDatetimeAxis) {
                        xData = Highcharts.map(xData, function (x) {
                            return Highcharts.dateFormat(dateFormat, x)
                            //return x;
                        });
                        //xTitle = 'DateTime';
                    } else if (series.xAxis.categories) {
                        xData = Highcharts.map(xData, function (x) {
                            return Highcharts.pick(series.xAxis.categories[x], x);
                        });
                        //xTitle = 'Category';
                    }
                    columns.push(xData);
                    columns[columns.length - 1].unshift(xTitle);
                }else if(series.data){
                	var xData=[],
                	    xTitle = 'X values';
                	each (series.data,function(item){
                		xData.push(item.name);
                	});
                	columns.push(xData);
                    columns[columns.length - 1].unshift(xTitle);
                }
                columns.push(series.yData.slice());
                columns[columns.length - 1].unshift('Y values');
            }
        });
        return columns;
    }
    
    Highcharts.Chart.prototype.getTotal = function() {
    	var columns=this.getData();
    	var sum=0;
    	for(i in columns[1]){
    		sum+=isNaN(parseInt(columns[1][i],10))?0:parseInt(columns[1][i],10);
    	}
    	return sum;
    }
    
    Highcharts.Chart.prototype.getCSV = function () {
        var columns = [],
            line,
            tempLine,
            csv = "", 
            row,
            col,
            options = (this.options.exporting || {}).csv || {},

            // Options
            itemDelimiter = options.itemDelimiter || ',', // use ';' for direct import to Excel
            lineDelimiter = options.lineDelimeter || '\n';

    	
		columns=this.getData();
        // Transform the columns to CSV
        for (row = 0; row < columns[0].length; row++) {
            line = [];
            for (col = 0; col < columns.length; col++) {
                line.push(columns[col][row]);
            }
            csv += line.join(itemDelimiter) + lineDelimiter;
        }

        return csv;
    };
 	Highcharts.Chart.prototype.exportCSV=function(){
 		 var chart=this;
 		 var content=chart.getCSV();
    	 var name = (chart.title ? chart.title.textStr.replace(/ /g, '-').toLowerCase() : 'chart');
    	 
    	 var downloadAttrSupported = document.createElement('a').download !== undefined;
    	 if (downloadAttrSupported) {
            a = document.createElement('a');
            a.href = 'data:text/csv,' + content.replace(/\n/g, '%0A');
            a.target      = '_blank';
            a.download    = name + '.csv';
            document.body.appendChild(a);
            a.click();
            a.remove();

        } else if (window.Blob && window.navigator.msSaveOrOpenBlob) {
            // Falls to msSaveOrOpenBlob if download attribute is not supported
            blobObject = new Blob([csv]);
            window.navigator.msSaveOrOpenBlob(blobObject, name + '.csv');

        } else{
            Highcharts.post('http://localhost:8080/hydrant-view/download/csv.do', {
                csv: content
            });
        }
 	}
 	// 以后拓展
 	Highcharts.Chart.prototype.getTable=function(){
 		var chart=this;
 		var columns=chart.getData();
 	}
    // Now we want to add "Download CSV" to the exporting menu. We post the CSV
    // to a simple PHP script that returns it with a content-type header as a 
    // downloadable file.
    // The source code for the PHP script can be viewed at 
    // https://raw.github.com/highslide-software/highcharts.com/master/studies/csv-export/csv.php
    if (Highcharts.getOptions().exporting) {
        Highcharts.getOptions().exporting.buttons.contextButton.menuItems.push({
            text: 'Download CSV',
            onclick: function () {
            	this.exportCSV();
            }
        });
    }
}(Highcharts));
