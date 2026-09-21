# Corrección de Navegación tras Pedido y Botón de Inicio

Se han identificado problemas en el flujo de navegación del comprador después de realizar un pedido. Específicamente, el usuario se siente "atrapado" en la pantalla de solicitudes y el botón de inicio (casita) en la barra inferior deja de funcionar correctamente en ciertos estados.

## Problemas Identificados
1. **Navegación Inconsistente**: Después de enviar un pedido (`BuyerRequestSentScreen`), solo existe la opción de ir a "Mis solicitudes". Al llegar ahí, la pila de navegación conserva pantallas intermedias o se confunde con el estado guardado.
2. **Botón de Inicio "Bloqueado"**: La lógica del `BuyerBottomBar` utiliza una comparación estricta (`currentRoute != route`) y un `popUpTo` que podría fallar si el estado de la pantalla principal no se guardó/restauró simétricamente en todos los puntos de navegación.
3. **Falta de opción de retorno directo**: La pantalla de éxito de la solicitud no ofrece un botón directo para volver al inicio, obligando al usuario a navegar por "Solicitudes" o usar el botón de retroceso del sistema.

## Cambios Propuestos

### Componentes de UI

#### [MODIFY] [BuyerScreens.kt](file:///C:/Users/kevin/Downloads/HarvestDistributionApp_REMEDIATED_v1.1.0/HarvestDistributionApp/app/src/main/java/com/example/harvestdistributionapp/ui/screens/BuyerScreens.kt)
- **`BuyerBottomBar`**: Refactorizar la lógica de navegación para usar el destino inicial del grafo de forma dinámica, asegurando que el botón de Inicio siempre funcione y limpie la pila correctamente.
- **`BuyerRequestSentScreen`**:
    - Añadir un botón "Volver al inicio" que lleve directamente a `BuyerHome` limpiando la pila.
    - Asegurar que el botón "Ver mis solicitudes" use las mismas opciones de navegación (`saveState`/`restoreState`) que la barra inferior para mantener la consistencia del `NavController`.

#### [MODIFY] [ProducerScreens.kt](file:///C:/Users/kevin/Downloads/HarvestDistributionApp_REMEDIATED_v1.1.0/HarvestDistributionApp/app/src/main/java/com/example/harvestdistributionapp/ui/screens/ProducerScreens.kt)
- **`ProducerBottomBar`**: Aplicar la misma mejora que en el comprador para evitar que el botón de inicio falle en el rol de productor.
- **`ProducerSuccessScreen`**: Asegurar consistencia en el botón "Volver al inicio".

## Plan de Verificación

### Pruebas Manuales
1. **Flujo de Compra**:
    - Realizar un pedido como comprador.
    - En la pantalla de éxito, verificar que el nuevo botón "Volver al inicio" funcione.
    - Probar el botón "Ver mis solicitudes" y luego intentar regresar al inicio usando el botón de la casita en la barra inferior.
2. **Navegación General**:
    - Alternar entre Inicio, Buscar y Solicitudes para asegurar que no hay duplicación de pantallas en la pila (el botón de retroceso del sistema debe llevar al inicio o salir de la app, no ciclar entre pestañas).
