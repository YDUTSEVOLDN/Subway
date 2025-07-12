const fs = require('fs');
const path = require('path');

const inputFile = path.join(__dirname, '..', 'subway.txt');
const outputDir = path.join(__dirname, '..', 'src', 'assets');
const outputFile = path.join(outputDir, 'subway-lines.json');

// 确保输出目录存在
if (!fs.existsSync(outputDir)){
    fs.mkdirSync(outputDir, { recursive: true });
}

// 转换高德内部坐标到经纬度
// 这是一个近似转换，基于对数据样本的观察和一些公开的转换算法
// 可能存在一定误差，但对于可视化足够
function convertGridToLonLat(gridCoords) {
    const coords = gridCoords.split(' ');
    const x = parseInt(coords[0]);
    const y = parseInt(coords[1]);
    
    // 转换参数 (基于经验值估算，可能需要微调)
    const lon_origin = 115.7; 
    const lat_origin = 40.25;
    const lon_scale = 0.000105;
    const lat_scale = -0.000086;
    
    const lon = lon_origin + x * lon_scale;
    const lat = lat_origin + y * lat_scale;
    
    return [lon, lat];
}

fs.readFile(inputFile, 'utf8', (err, data) => {
    if (err) {
        console.error("Error reading subway.txt:", err);
        return;
    }

    try {
        const subwayData = JSON.parse(data);
        const lines = subwayData.l;

        const processedLines = lines.map(line => {
            const lineName = line.kn && !line.kn.includes('地铁') ? line.kn : line.ln;
            
            const stations = line.st.map(station => {
                const [lon, lat] = station.sl.split(',');
                return {
                    name: station.n,
                    coord: [parseFloat(lon), parseFloat(lat)]
                };
            });

            // 提取并转换详细路径
            let detailedPath = [];
            if (line.lp && line.lp.length > 0) {
                 // lp字段本身是一个数组，里面可能包含多段路径字符串
                 line.lp.forEach(pathSegment => {
                    // 每段路径是用分号隔开的坐标对
                    const pathCoords = pathSegment.split(';');
                    pathCoords.forEach(coord => {
                        if(coord) {
                            detailedPath.push(convertGridToLonLat(coord));
                        }
                    });
                });
            } else {
                // 如果没有详细路径，则退回使用站点连接
                detailedPath = stations.map(s => s.coord);
            }

            return {
                name: lineName,
                color: `#${line.cl}`,
                stations: stations,
                path: detailedPath // 使用新的详细路径
            };
        });

        fs.writeFile(outputFile, JSON.stringify(processedLines, null, 2), 'utf8', (err) => {
            if (err) {
                console.error("Error writing subway-lines.json:", err);
            } else {
                console.log("subway-lines.json has been created successfully with detailed paths!");
            }
        });

    } catch (parseErr) {
        console.error("Error parsing JSON from subway.txt:", parseErr);
    }
}); 