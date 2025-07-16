const fs = require('fs');
const mysql = require('mysql2/promise');
const path = require('path');

// 从 subway-lines.json 中提取所有站点
function getAllStations() {
  const jsonPath = path.join(__dirname, '..', 'src', 'assets', 'subway-lines.json');
  const subwayData = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const allStations = new Map();

  subwayData.forEach(line => {
    line.stations.forEach(station => {
      // 使用 Map 来处理重复的站点（如换乘站），确保每个站点只记录一次
      if (!allStations.has(station.name)) {
        allStations.set(station.name, {
          name: station.name,
          lng: station.coord[0],
          lat: station.coord[1]
        });
      }
    });
  });

  return Array.from(allStations.values());
}

// 更新数据库
async function updateDatabase(stations) {
  // 从 application.yml 获取的数据库配置
  const dbConfig = {
    host: '39.96.195.232',
    user: 'admin',
    password: 'password',
    database: 'subbike',
    port: 3306 
  };

  let connection;
  try {
    connection = await mysql.createConnection(dbConfig);
    console.log('成功连接到数据库！');

    let updatedCount = 0;
    let notFoundCount = 0;
    const notFoundStations = [];

    for (const station of stations) {
      const { name, lat, lng } = station;
      const [result] = await connection.execute(
        'UPDATE stations SET lat = ?, lng = ? WHERE name = ?',
        [lat, lng, name]
      );

      if (result.affectedRows > 0) {
        console.log(`已更新站点: ${name}`);
        updatedCount++;
      } else {
        console.warn(`警告: 未在数据库中找到站点: ${name}`);
        notFoundCount++;
        notFoundStations.push(name);
      }
    }

    console.log('\n--- 更新完成 ---');
    console.log(`成功更新 ${updatedCount} 个站点。`);
    console.log(`未找到 ${notFoundCount} 个站点。`);
    if (notFoundStations.length > 0) {
        console.log('未找到的站点列表:', notFoundStations.join(', '));
    }

  } catch (error) {
    console.error('数据库操作失败:', error);
  } finally {
    if (connection) {
      await connection.end();
      console.log('数据库连接已关闭。');
    }
  }
}

// 主函数
async function main() {
  const stations = getAllStations();
  console.log(`从 JSON 文件中总共提取了 ${stations.length} 个独立站点。`);
  await updateDatabase(stations);
}

main(); 