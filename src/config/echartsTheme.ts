import * as echarts from 'echarts';

const theme = {
  color: [
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'
  ],
  backgroundColor: 'rgba(255, 255, 255, 0)',
  textStyle: {
    fontFamily: 'Inter, "Helvetica Neue", Arial, sans-serif',
    fontSize: 12,
    color: '#333'
  },
  title: {
    textStyle: {
      color: '#333',
      fontWeight: 'bold',
      fontSize: 16
    }
  },
  legend: {
    textStyle: {
      color: '#333'
    }
  },
  tooltip: {
    backgroundColor: 'rgba(50,50,50,0.7)',
    borderColor: '#333',
    textStyle: {
      color: '#fff'
    }
  },
  grid: {
    borderColor: '#eee'
  },
  xAxis: {
    axisLine: {
      lineStyle: {
        color: '#ccc'
      }
    },
    axisLabel: {
      color: '#666'
    }
  },
  yAxis: {
    axisLine: {
      lineStyle: {
        color: '#ccc'
      }
    },
    splitLine: {
      lineStyle: {
        color: ['#eee']
      }
    },
    axisLabel: {
      color: '#666'
    }
  },
  line: {
    smooth: true,
    symbol: 'circle',
    symbolSize: 6
  },
  bar: {
    barWidth: '40%',
    itemStyle: {
      borderRadius: [4, 4, 0, 0]
    }
  }
};

export const registerTheme = () => {
  echarts.registerTheme('customTheme', theme);
}; 