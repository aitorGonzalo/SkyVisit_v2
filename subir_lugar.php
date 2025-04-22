<?php
header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Método no permitido"]);
    exit;
}

// Comprobar campos necesarios
if (!isset($_POST['nombre'], $_POST['latitud'], $_POST['longitud'], $_POST['usuario_id'], $_FILES['foto'])) {
    http_response_code(400);
    echo json_encode(["error" => "Faltan datos"]);
    exit;
}

$nombre = $_POST['nombre'];
$latitud = $_POST['latitud'];
$longitud = $_POST['longitud'];
$usuario_id = $_POST['usuario_id'];

// Guardar imagen
// Guardar imagen
$nombre_foto = time() . "_" . basename($_FILES['foto']['name']);
$ruta_destino = "fotos/" . $nombre_foto;

// Comprobaciones adicionales para depuración
if (!isset($_FILES['foto']) || $_FILES['foto']['error'] !== 0) {
    http_response_code(400);
    echo json_encode(["error" => "⚠️ Error en el archivo de imagen: " . $_FILES['foto']['error']]);
    exit;
}

if (!is_uploaded_file($_FILES['foto']['tmp_name'])) {
    http_response_code(400);
    echo json_encode(["error" => "⚠️ El archivo no es válido o no se subió correctamente"]);
    exit;
}

if (!move_uploaded_file($_FILES['foto']['tmp_name'], $ruta_destino)) {
    http_response_code(500);
    echo json_encode(["error" => "❌ Error al mover la imagen al destino"]);
    exit;
}

// Conexión a BD
$conexion = new mysqli("localhost", "Xagonzalo021", "2HrrHmtvc2", "Xagonzalo021_lugares");
if ($conexion->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión"]);
    exit;
}

// Insertar en BD
$sql = $conexion->prepare("INSERT INTO lugares (nombre, latitud, longitud, foto, usuario_id) VALUES (?, ?, ?, ?, ?)");
$sql->bind_param("sddsi", $nombre, $latitud, $longitud, $nombre_foto, $usuario_id);

if ($sql->execute()) {
    echo json_encode(["resultado" => "ok"]);
} else {
    http_response_code(500);
    echo json_encode(["error" => "Error al insertar en la base de datos"]);
}

$sql->close();
$conexion->close();
?>
