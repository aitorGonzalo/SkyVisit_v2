<?php
header("Content-Type: application/json");

// Conexión a la base de datos
$conexion = new mysqli("localhost", "Xagonzalo021", "2HrrHmtvc2", "Xagonzalo021_lugares");

if ($conexion->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión"]);
    exit;
}

// Obtener los lugares con sus datos completos
$sql = "SELECT nombre, latitud, longitud, foto FROM lugares";
$resultado = $conexion->query($sql);

$lugares = [];
while ($row = $resultado->fetch_assoc()) {
    $lugares[] = $row;
}

echo json_encode($lugares);

$conexion->close();
?>
