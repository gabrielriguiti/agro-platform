/** Barra do {@link BarChart}. */
export interface BarDatum {
  label: string;
  value: number;
}

export interface BarChartProps {
  /** Dados na ordem em que devem aparecer (de cima para baixo). */
  data: BarDatum[];
  /** Descrição acessível do gráfico. */
  title: string;
}

const WIDTH = 760;
const LABEL_WIDTH = 220;
const VALUE_PAD = 36;
const ROW = 36;
const BAR = 18;
const TOP = 8;
const AXIS = 28;

/** Passos "redondos" do eixo, de 0 até um valor que cobre `max` (no máximo cerca de 6 marcas). */
export function niceTicks(max: number): number[] {
  if (max <= 0) return [0, 1];
  const rough = max / 5;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 5, 10].map((m) => m * magnitude).find((candidate) => candidate >= rough) ?? magnitude * 10;
  const ticks: number[] = [];
  for (let value = 0; value < max + step; value += step) {
    ticks.push(value);
    if (value >= max) break;
  }
  return ticks;
}

/**
 * Gráfico de barras horizontais em SVG simples, sem biblioteca. Mantém a ordem recebida.
 */
export function BarChart({ data, title }: BarChartProps) {
  const ticks = niceTicks(Math.max(0, ...data.map((d) => d.value)));
  const top = ticks[ticks.length - 1];
  const plot = WIDTH - LABEL_WIDTH - VALUE_PAD;
  const height = TOP + data.length * ROW + AXIS;
  const x = (value: number) => LABEL_WIDTH + (value / top) * plot;
  return (
    <svg viewBox={`0 0 ${WIDTH} ${height}`} role="img" aria-label={title} focusable="false">
      <title>{title}</title>
      {ticks.map((tick) => (
        <g key={tick}>
          <line x1={x(tick)} x2={x(tick)} y1={TOP} y2={TOP + data.length * ROW} stroke="var(--divider)" />
          <text x={x(tick)} y={height - 8} textAnchor="middle">
            {tick}
          </text>
        </g>
      ))}
      {data.map((datum, index) => {
        const y = TOP + index * ROW + (ROW - BAR) / 2;
        const width = Math.max(0, x(datum.value) - LABEL_WIDTH);
        return (
          <g key={`${datum.label}-${index}`} data-testid="bar">
            <text x={LABEL_WIDTH - 12} y={y + BAR / 2 + 4} textAnchor="end">
              {datum.label}
            </text>
            <rect x={LABEL_WIDTH} y={y} width={width} height={BAR} rx={2} fill={index === 0 ? "var(--pri)" : "var(--pri-chart)"} />
            <text className="chart__value" x={LABEL_WIDTH + width + 8} y={y + BAR / 2 + 4}>
              {datum.value}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
