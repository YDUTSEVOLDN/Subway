const fs = require('fs');
const path = require('path');
const Papa = require('papaparse');

const csvFilePath = path.join(__dirname, '../src/assets/subway_bike_counts.csv');
const jsonFilePath = path.join(__dirname, '../src/assets/bike_data.json');

const csvFile = fs.readFileSync(csvFilePath, 'utf8');

Papa.parse(csvFile, {
  header: true,
  complete: (results) => {
    const stationData = {};
    // Filter out any potential empty rows from parsing
    const validData = results.data.filter(row => row.station_name && row.bike_count);

    validData.forEach(row => {
        const stationName = row.station_name.trim();
        const bikeCount = parseInt(row.bike_count, 10);
        if (stationName && !isNaN(bikeCount)) {
            stationData[stationName] = bikeCount;
        }
    });

    fs.writeFileSync(jsonFilePath, JSON.stringify(stationData, null, 2), 'utf8');
    console.log(`Successfully processed CSV and created ${jsonFilePath}`);
  },
  error: (error) => {
    console.error("Error parsing CSV:", error);
  }
});
