<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

// Obtener datos POST
data = json_decode(file_get_contents("php://input"));

// Verificar datos requeridos
if(!isset($data->license_key) || !isset($data->hwid)) {
    http_response_code(400);
    echo json_encode(array("message" => "Datos incompletos"));
    exit();
}

// Conexión a la base de datos (ajustar según tu configuración)
$servername = "localhost";
$username = "tu_usuario_db";
$password = "tu_password_db";
$dbname = "tu_base_de_datos";

$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de conexión"));
    exit();
}

// Consulta SQL segura
$stmt = $conn->prepare("SELECT * FROM licenses WHERE license_key = ?");
$stmt->bind_param("s", $data->license_key);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $license = $result->fetch_assoc();
    
    // Verificar si la licencia está activa (is_used = 1)
    $is_valid = ($license['is_used'] == 1);
    
    if ($is_valid) {
        // Si la licencia no tiene HWID, asignarle uno
        if (empty($license['hwid']) || $license['hwid'] == 'NULL') {
            $update_stmt = $conn->prepare("UPDATE licenses SET hwid = ? WHERE license_key = ?");
            $update_stmt->bind_param("ss", $data->hwid, $data->license_key);
            $update_stmt->execute();
            
            echo json_encode(array(
                "valid" => true,
                "hwid_match" => true,
                "product_type" => $license['product_type']
            ));
        } 
        // Si ya tiene HWID, verificar que coincida
        else {
            $hwid_match = ($license['hwid'] == $data->hwid);
            
            echo json_encode(array(
                "valid" => true,
                "hwid_match" => $hwid_match,
                "product_type" => $license['product_type']
            ));
        }
    } else {
        // Licencia no activada
        echo json_encode(array(
            "valid" => false,
            "hwid_match" => false,
            "message" => "Licencia no activada"
        ));
    }
} else {
    // Licencia no encontrada
    echo json_encode(array(
        "valid" => false,
        "hwid_match" => false,
        "message" => "Licencia no encontrada"
    ));
}

$stmt->close();
$conn->close();
?>