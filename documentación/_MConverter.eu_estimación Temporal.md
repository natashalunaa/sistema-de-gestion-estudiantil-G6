En este documento se encuentra la estimación temporal del proyecto, asumiendo un modelo ideal inicial sin considerar el producto final real, usando las técnicas de Cálculo de Puntos de Función y COCOMO Básico

Valores de ajuste de complejidad:

| Factor | Valor |
|----|----|
| ¿Requiere el sistema copias de seguridad y recuperación viables? | 1 |
| ¿Se Requiere comunicación de datos? | 2 |
| ¿Existen funciones de procesamiento distribuido? | 0 |
| ¿Es crítico el funcionamiento? | 2 |
| ¿Se ejecutará el sistema en un entorno operativo existente fuertemente utilizado? | 0 |
| ¿Requiere el sistema entrada de datos interactiva? | 3 |
| ¿Requiere la entrada de datos interactiva que las transacciones de entrada se lleven a cabo sobre múltiples pantallas u operaciones? | 1 |
| ¿Se actualizan los archivos maestros de forma interactiva? | 0 |
| ¿Son complejas las entradas, las salidas, los archivos o las peticiones? | 1 |
| ¿Es complejo el procesamiento interno? | 2 |
| ¿Se ha diseñado el código para que sea reutilizable? | 1 |
| ¿Están incluidas en el diseño la conversión y la instalación? | 0 |
| ¿Se ha diseñado el sistema para soportar múltiples instalaciones en diferentes organizaciones? | 0 |
| ¿Se ha diseñado la aplicación para facilitar los cambios y para ser fácilmente utilizada por el usuario? | 2 |

Puntos de Función:

Se toma un factor de ponderación simple

<table style="width:100%;">
<colgroup>
<col style="width: 20%" />
<col style="width: 38%" />
<col style="width: 13%" />
<col style="width: 18%" />
<col style="width: 8%" />
</colgroup>
<thead>
<tr>
<th>Parámetro de Medición</th>
<th>Funcionalidad Identificada</th>
<th>Cantidad</th>
<th>Factor de Ponderación</th>
<th>Total</th>
</tr>
</thead>
<tbody>
<tr>
<td>Entradas de Usuario</td>
<td><ul>
<li><p>ABM Docente</p></li>
<li><p>ABM Estudiante</p></li>
<li><p>ABM Administrador</p></li>
<li><p>ABM Notas</p></li>
<li><p>ABM Materia</p></li>
</ul></td>
<td>15</td>
<td>3</td>
<td>45</td>
</tr>
<tr>
<td>Salidas de Usuario</td>
<td><ul>
<li><p>Rendimiento academico</p></li>
<li><p>Listado de Docentes</p></li>
<li><p>Listado de Estudiantes</p></li>
<li><p>Listado de Materias</p></li>
</ul></td>
<td>4</td>
<td>4</td>
<td>16</td>
</tr>
<tr>
<td>Peticiones de Usuario</td>
<td><ul>
<li><p>Búsqueda de docente</p></li>
<li><p>Búsqueda de materia</p></li>
<li><p>Búsqueda de estudiante</p></li>
</ul></td>
<td>3</td>
<td>3</td>
<td>9</td>
</tr>
<tr>
<td>Archivos</td>
<td><ul>
<li><p>Estudiante</p></li>
<li><p>Docente</p></li>
<li><p>Administrador</p></li>
<li><p>Materia</p></li>
<li><p>Nota</p></li>
</ul></td>
<td>5</td>
<td>7</td>
<td>35</td>
</tr>
<tr>
<td>Interfaces externas</td>
<td></td>
<td>0</td>
<td>5</td>
<td>0</td>
</tr>
</tbody>
</table>

Esto nos da un total de Puntos de Función sin ajustar de 105

Puntos de Función Ajustados = 105 \* (0,65 + 0,01 \* 15) = 84

Ahora, dado que la mayoría del proyecto será escrito en lenguaje Java, y teniendo un promedio de LOC = PF \* 53, podemos predecir que el producto tendrá alrededor de 4.452 líneas de código.

COCOMO:

Usando COCOMO Básico en su modelo Orgánico, obtenemos los siguientes resultados:

E = 2,4 \* 4,4^1,05^ = 11,3

D = 2,5 \* 11,3^0,38^ = 6,2 meses

Es así como podemos asumir que el proyecto tomará alrededor de 6 meses
