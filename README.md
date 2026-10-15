# DocuFind (Proyecto original: Biblio Tecno)

**DocuFind** (inscrita formalmente bajo el nombre *Biblio Tecno*[cite: 4]) es una aplicación móvil nativa para Android estructurada como una red social documental. Permite a los usuarios subir, organizar y compartir documentos consolidando un ecosistema donde estudiantes y profesionales pueden acceder a apuntes, manuales y material de estudio mediante un sistema de comunidades.

Este proyecto fue desarrollado como parte de la asignatura de **Programación Android** en la sede **Santo Tomás Iquique**

## 🎯 Propósito y Audiencia
La aplicación está diseñada para estudiantes, académicos y profesionales que requieren una plataforma centralizada para el intercambio de conocimientos. Su objetivo principal es ofrecer una "red" de acceso libre donde los usuarios pueden buscar, filtrar y consumir documentos de interés subidos por otros miembros de la comunidad

## ✨ Características Principales
* **Feed Global de la Comunidad:** Un muro interactivo para explorar todas las publicaciones activas en la red, optimizado con un motor de búsqueda por coincidencia de texto y filtros dinámicos por categoría.
* **Muro Personal y Gestión:** Un entorno privado donde cada usuario administra sus aportes. Permite editar textos, actualizar categorías, re-adjuntar archivos y eliminar publicaciones mediante un sistema de borrado lógico (*Soft Delete*).
* **Soporte Multimedia Dual:** Los usuarios pueden crear publicaciones adjuntando documentos PDF desde su almacenamiento interno, o utilizar el sensor de la cámara del dispositivo para capturar e integrar fotografías de apuntes físicos en tiempo real.
* **Visualización Segura:** Implementación de `FileProvider` para aislar los archivos locales y delegar su apertura a los lectores o visores nativos del dispositivo mediante URIs seguras y permisos temporales.

## 🛠️ Stack Tecnológico y Arquitectura
* **Lenguaje de Programación:** Java
* **Entorno de Desarrollo:** Android Studio
* **Base de Datos Local:** SQLite. Sistema relacional normalizado compuesto por 6 tablas (Usuarios, Roles, Publicaciones, Documentos, Categorías, Fotos) con políticas estrictas de integridad referencial.
* **Arquitectura de Interfaz:** Modelo de actividad única (*Single-Activity*). Un `MainActivity` central gestiona la reutilización de la interfaz delegando la navegación a múltiples `Fragments` (`HomeFragment`, `WebFragment`), garantizando una interfaz sencilla e intuitiva[cite: 4].
* **Gestión de Componentes Avanzados:** Uso exclusivo de contratos modernos (`ActivityResultLaunchers`) para el manejo asíncrono de Intents, captura de imágenes, selectores de archivos y solicitud de permisos.

## 🔒 Gestión de Permisos
En cumplimiento con la planificación del proyecto, DocuFind gestiona de forma segura los siguientes permisos en tiempo de ejecución (*Runtime Permissions*):
* **Acceso a Archivos:** Fundamental para la exploración, selección y copiado de documentos PDF desde el almacenamiento del usuario.
* **Sensor de Cámara (`android.permission.CAMERA`):** Requerido para la captura directa de fotografías dentro del flujo de creación de publicaciones.

## 👨‍💻 Autor e Institución
* **Desarrollador:** Miguel Castillo
* **Docente a Cargo:** Leopoldo Rodriguez Esteban Bravo
* **Institución:** Santo Tomás Iquique
