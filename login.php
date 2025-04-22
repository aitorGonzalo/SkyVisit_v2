<?php
header("Content-Type: application/json");
$input = json_decode(file_get_contents("php://input"), true);

$usuario = $input["usuario"];
$password = $input["password"];

$conexion = new mysqli("localhost", "Xagonzalo021", "2HrrHmtvc2", "Xagonzalo021_lugares");
if ($conexion->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión"]);
    exit;
}

$sql = $conexion->prepare("SELECT id, password FROM usuarios WHERE usuario = ?");
$sql->bind_param("s", $usuario);
$sql->execute();
$resultado = $sql->get_result();

if ($fila = $resultado->fetch_assoc()) {
    if (password_verify($password, $fila["password"])) {
        echo json_encode(["resultado" => "ok", "id" => $fila["id"]]);
    } else {
        http_response_code(401);
        echo json_encode(["error" => "Contraseña incorrecta"]);
    }
} else {
    http_response_code(404);
    echo json_encode(["error" => "Usuario no encontrado"]);
}

$sql->close();
$conexion->close();
?>
