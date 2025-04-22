<?php
header("Content-Type: application/json");

// Leer JSON recibido
$input = json_decode(file_get_contents("php://input"), true);

// Comprobar que llegan los parámetros
if (!isset($input["usuario"]) || !isset($input["password"])) 
    {
    http_response_code(400);
    echo json_encode(["error" => "Faltan datos"]);
    exit;
}

$usuario = $input["usuario"];
$password = password_hash($input["password"], PASSWORD_DEFAULT);

// Conexión a la base de datos
$conexion = new mysqli("localhost", "Xagonzalo021", "2HrrHmtvc2", "Xagonzalo021_lugares");
if ($conexion->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión"]);
    exit;
}

// Preparar e insertar
// Comprobar si el usuario ya existe
$check = $conexion->prepare("SELECT id FROM usuarios WHERE usuario = ?");
$check->bind_param("s", $usuario);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    http_response_code(400);
    echo json_encode(["error" => "Usuario ya registrado"]);
    exit;
}
$check->close();

// Preparar e insertar
// Comprobar si el usuario ya existe
$check = $conexion->prepare("SELECT id FROM usuarios WHERE usuario = ?");
$check->bind_param("s", $usuario);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    http_response_code(409); // 409 = conflicto (usuario ya existe)
    echo json_encode(["error" => "Usuario ya registrado"]);
    exit;
}
$check->close();

// Insertar nuevo usuario
$sql = $conexion->prepare("INSERT INTO usuarios (usuario, password) VALUES (?, ?)");
if (!$sql) {
    http_response_code(500);
    echo json_encode(["error" => "Error al preparar la consulta"]);
    exit;
}
$sql->bind_param("ss", $usuario, $password);

if ($sql->execute()) {
    echo json_encode(["resultado" => "ok"]);
} else {
    http_response_code(500);
    echo json_encode(["error" => "Error al insertar el usuario"]);
}



$sql->close();
$conexion->close();
?>
