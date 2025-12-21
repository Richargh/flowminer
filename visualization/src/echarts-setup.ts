/**
 * ECharts Setup with Tree-Shaking
 *
 * This module exports a configured ECharts instance with only the
 * components needed for our visualization charts.
 */
import * as echarts from 'echarts/core';

// Chart types
import { BarChart, LineChart, RadarChart, ScatterChart, SankeyChart } from 'echarts/charts';

// Components
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
  MarkLineComponent,
} from 'echarts/components';

// Renderer
import { CanvasRenderer } from 'echarts/renderers';

// Register components
echarts.use([
  // Charts
  BarChart,
  LineChart,
  RadarChart,
  ScatterChart,
  SankeyChart,
  // Components
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
  MarkLineComponent,
  // Renderer
  CanvasRenderer,
]);

export { echarts };
