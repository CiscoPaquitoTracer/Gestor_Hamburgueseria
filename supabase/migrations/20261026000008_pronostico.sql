-- =====================================================================
-- Pronóstico de ventas e insumos, para avisar qué reabastecer antes de un
-- fin de semana o un día festivo.
--
-- Con el historial se estima cada día por separado:
--   * festivo → como los festivos anteriores (último año);
--   * si no   → como el mismo día de la semana (sin festivos) de las últimas 12 semanas;
--   * sin historial de ese tipo → promedio de todos los días con ventas de las últimas 12 semanas.
-- Solo cuentan los días que hubo ventas (los días cerrados no bajan el promedio).
-- =====================================================================

-- Calendario de festivos ----------------------------------------------------
CREATE TABLE dias_festivos (
    fecha    date    PRIMARY KEY,
    nombre   text    NOT NULL,
    -- true = descanso obligatorio (Ley Federal del Trabajo, art. 74); false = fecha comercial.
    oficial  boolean NOT NULL DEFAULT false
);
COMMENT ON TABLE dias_festivos IS 'Se pueden agregar fechas propias (ej. feria del pueblo) desde el panel de Supabase.';

ALTER TABLE dias_festivos ENABLE ROW LEVEL SECURITY;
CREATE POLICY equipo_todo ON dias_festivos FOR ALL TO authenticated
    USING ((SELECT es_usuario_permitido())) WITH CHECK ((SELECT es_usuario_permitido()));
REVOKE ALL ON dias_festivos FROM anon;

-- n-ésimo día de la semana de un mes (isodow: 1 = lunes … 7 = domingo).
CREATE FUNCTION pg_temp.enesimo(p_anio int, p_mes int, p_isodow int, p_n int) RETURNS date
LANGUAGE sql IMMUTABLE AS $$
    SELECT make_date(p_anio, p_mes, 1)
         + (p_isodow - extract(isodow FROM make_date(p_anio, p_mes, 1))::int + 7) % 7
         + (p_n - 1) * 7
$$;

-- Domingo de Pascua (algoritmo gregoriano anónimo), para Semana Santa.
CREATE FUNCTION pg_temp.pascua(y int) RETURNS date LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE a int; b int; c int; d int; e int; f int; g int; h int; i int; k int; l int; m int;
BEGIN
    a := y % 19; b := y / 100; c := y % 100; d := b / 4; e := b % 4;
    f := (b + 8) / 25; g := (b - f + 1) / 3; h := (19 * a + b - d - g + 15) % 30;
    i := c / 4; k := c % 4; l := (32 + 2 * e + 2 * i - h - k) % 7;
    m := (a + 11 * h + 22 * l) / 451;
    RETURN make_date(y, (h + l - 7 * m + 114) / 31, (h + l - 7 * m + 114) % 31 + 1);
END $$;

INSERT INTO dias_festivos (fecha, nombre, oficial)
SELECT f.fecha, f.nombre, f.oficial
  FROM generate_series(2025, 2030) AS y (a)
 CROSS JOIN LATERAL (VALUES
    (make_date(a, 1, 1),            'Año Nuevo',                      true),
    (pg_temp.enesimo(a, 2, 1, 1),   'Día de la Constitución',         true),
    (make_date(a, 2, 14),           'San Valentín',                   false),
    (pg_temp.enesimo(a, 3, 1, 3),   'Natalicio de Benito Juárez',     true),
    (pg_temp.pascua(a) - 3,         'Jueves Santo',                   false),
    (pg_temp.pascua(a) - 2,         'Viernes Santo',                  false),
    (make_date(a, 4, 30),           'Día del Niño',                   false),
    (make_date(a, 5, 1),            'Día del Trabajo',                true),
    (make_date(a, 5, 10),           'Día de las Madres',              false),
    (pg_temp.enesimo(a, 6, 7, 3),   'Día del Padre',                  false),
    (make_date(a, 9, 15),           'Noche Mexicana',                 false),
    (make_date(a, 9, 16),           'Día de la Independencia',        true),
    (make_date(a, 11, 2),           'Día de Muertos',                 false),
    (pg_temp.enesimo(a, 11, 1, 3),  'Revolución Mexicana',            true),
    (make_date(a, 12, 12),          'Día de la Virgen de Guadalupe',  false),
    (make_date(a, 12, 24),          'Nochebuena',                     false),
    (make_date(a, 12, 25),          'Navidad',                        true),
    (make_date(a, 12, 31),          'Fin de Año',                     false)
 ) AS f (fecha, nombre, oficial)
ON CONFLICT (fecha) DO NOTHING;

INSERT INTO dias_festivos (fecha, nombre, oficial)
VALUES ('2030-10-01', 'Transmisión del Poder Ejecutivo', true)
ON CONFLICT (fecha) DO NOTHING;

-- Historial por día (fecha de México) ---------------------------------------
CREATE VIEW v_ventas_por_dia WITH (security_invoker = true) AS
SELECT (fecha AT TIME ZONE 'America/Mexico_City')::date AS dia, count(*) AS pedidos
  FROM ventas
 WHERE estado <> 'cancelada'
 GROUP BY 1;

-- Insumo gastado por día, según la fecha del pedido (salidas por venta, incluidos los combos).
CREATE VIEW v_consumo_por_dia WITH (security_invoker = true) AS
SELECT (v.fecha AT TIME ZONE 'America/Mexico_City')::date AS dia, m.insumo_id, -sum(m.cantidad) AS cantidad
  FROM movimientos_inventario m
  LEFT JOIN venta_componentes c ON c.id = m.venta_componente_id
  JOIN venta_detalle d          ON d.id = coalesce(m.venta_detalle_id, c.detalle_id)
  JOIN ventas v                 ON v.id = d.venta_id
 WHERE m.tipo = 'venta'
 GROUP BY 1, 2;

-- Días del historial con los que se estima p_fecha (ver reglas arriba).
CREATE FUNCTION interno.dias_referencia(p_fecha date) RETURNS TABLE (dia date)
LANGUAGE plpgsql STABLE SET search_path = public AS $$
#variable_conflict use_column
DECLARE hoy date := (now() AT TIME ZONE 'America/Mexico_City')::date;
BEGIN
    IF EXISTS (SELECT 1 FROM dias_festivos f WHERE f.fecha = p_fecha) THEN
        RETURN QUERY
            SELECT v.dia FROM v_ventas_por_dia v JOIN dias_festivos f ON f.fecha = v.dia
             WHERE v.dia >= hoy - 365 AND v.dia < hoy;
        IF FOUND THEN RETURN; END IF;
    END IF;

    RETURN QUERY
        SELECT v.dia FROM v_ventas_por_dia v
         WHERE v.dia >= hoy - 84 AND v.dia < hoy
           AND extract(isodow FROM v.dia) = extract(isodow FROM p_fecha)
           AND NOT EXISTS (SELECT 1 FROM dias_festivos f WHERE f.fecha = v.dia);
    IF FOUND THEN RETURN; END IF;

    RETURN QUERY
        SELECT v.dia FROM v_ventas_por_dia v WHERE v.dia >= hoy - 84 AND v.dia < hoy;
END $$;

-- API de la app -------------------------------------------------------------

-- Pedidos esperados para los próximos p_dias (empezando hoy).
-- pedidos_promedio = promedio de todos los días con ventas (12 semanas), para comparar.
-- dias_historial = cuántos días del pasado respaldan la estimación (0 = sin datos).
-- dia_semana: 1 = lunes … 7 = domingo.
CREATE FUNCTION pronostico_dias(p_dias int DEFAULT 7)
RETURNS TABLE (fecha date, dia_semana int, festivo text, pedidos_esperados numeric, pedidos_promedio numeric,
               dias_historial int)
LANGUAGE sql STABLE SET search_path = public, interno AS $$
    WITH hoy AS (SELECT (now() AT TIME ZONE 'America/Mexico_City')::date AS d),
    dias AS (
        SELECT g::date AS fecha FROM hoy, generate_series(hoy.d, hoy.d + least(greatest(p_dias, 1), 31) - 1, interval '1 day') AS g
    ),
    refs AS (SELECT d.fecha, r.dia FROM dias d CROSS JOIN LATERAL dias_referencia(d.fecha) r),
    promedio AS (
        SELECT avg(v.pedidos) AS p FROM v_ventas_por_dia v, hoy WHERE v.dia >= hoy.d - 84 AND v.dia < hoy.d
    )
    SELECT d.fecha, extract(isodow FROM d.fecha)::int, f.nombre,
           round(avg(v.pedidos), 1),
           round((SELECT p FROM promedio), 1),
           count(refs.dia)::int
      FROM dias d
      LEFT JOIN dias_festivos f ON f.fecha = d.fecha
      LEFT JOIN refs            ON refs.fecha = d.fecha
      LEFT JOIN v_ventas_por_dia v ON v.dia = refs.dia
     GROUP BY d.fecha, f.nombre
     ORDER BY d.fecha
$$;

-- Insumo que se espera gastar desde hoy hasta p_hasta (incluido), contra lo que hay.
-- De hoy solo cuenta lo que falta por vender (lo esperado menos lo ya vendido hoy).
-- por_comprar = lo necesario para cubrir esos días y quedar al menos en el stock mínimo.
CREATE FUNCTION pronostico_insumos(p_hasta date)
RETURNS TABLE (insumo_id bigint, nombre text, unidad text, existencia numeric, stock_minimo numeric,
               consumo_esperado numeric, por_comprar numeric)
LANGUAGE sql STABLE SET search_path = public, interno AS $$
    WITH hoy AS (SELECT (now() AT TIME ZONE 'America/Mexico_City')::date AS d),
    dias AS (
        SELECT g::date AS fecha FROM hoy, generate_series(hoy.d, least(greatest(p_hasta, hoy.d), hoy.d + 30), interval '1 day') AS g
    ),
    refs AS (SELECT d.fecha, r.dia FROM dias d CROSS JOIN LATERAL dias_referencia(d.fecha) r),
    n AS (SELECT fecha, count(*) AS n FROM refs GROUP BY fecha),
    -- Promedio por día de referencia; si ese día no se usó el insumo, cuenta como 0.
    esperado AS (
        SELECT refs.fecha, c.insumo_id, sum(c.cantidad) / n.n AS cantidad
          FROM refs JOIN n ON n.fecha = refs.fecha
          JOIN v_consumo_por_dia c ON c.dia = refs.dia
         GROUP BY refs.fecha, c.insumo_id, n.n
    ),
    total AS (
        SELECT e.insumo_id,
               sum(CASE WHEN e.fecha = hoy.d THEN greatest(e.cantidad - coalesce(g.cantidad, 0), 0)
                        ELSE e.cantidad END) AS cantidad
          FROM esperado e
         CROSS JOIN hoy
          LEFT JOIN v_consumo_por_dia g ON g.dia = hoy.d AND g.insumo_id = e.insumo_id
         GROUP BY e.insumo_id
    )
    SELECT i.id, i.nombre, i.unidad, i.cantidad, i.stock_minimo,
           round(t.cantidad, 3),
           round(greatest(t.cantidad + i.stock_minimo - i.cantidad, 0), 3)
      FROM total t JOIN insumos i ON i.id = t.insumo_id
     WHERE i.activo AND t.cantidad > 0
     ORDER BY greatest(t.cantidad + i.stock_minimo - i.cantidad, 0) DESC, i.nombre
$$;

REVOKE EXECUTE ON FUNCTION pronostico_dias(int), pronostico_insumos(date), interno.dias_referencia(date)
    FROM public, anon;
GRANT EXECUTE ON FUNCTION pronostico_dias(int), pronostico_insumos(date), interno.dias_referencia(date)
    TO authenticated;
