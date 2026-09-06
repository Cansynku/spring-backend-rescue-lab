param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
if ($BaseUrl -notmatch '^http://(127\.0\.0\.1|localhost)(:\d+)?$') {
    throw 'This demo script only targets localhost.'
}
$created = Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/orders" -ContentType 'application/json' -Body '{"customerEmail":"demo@example.com","totalAmount":25.50}'
if ($created.StatusCode -ne 201) { throw 'Expected order HTTP 201' }
$order = $created.Content | ConvertFrom-Json
if ($order.status -ne 'CREATED') { throw 'Expected CREATED order' }
$paymentResult = Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/orders/$($order.id)/payments" -ContentType 'application/json' -Body '{"amount":25.50}'
if ($paymentResult.StatusCode -ne 201) { throw 'Expected payment HTTP 201' }
$payment = $paymentResult.Content | ConvertFrom-Json
if ($payment.status -ne 'AUTHORIZED' -or $payment.providerPaymentId -notlike 'sandbox-*') { throw 'Expected simulated authorization' }
$read = Invoke-RestMethod "$BaseUrl/api/orders/$($order.id)"
if ($read.status -ne 'PAID' -or $read.paymentCount -ne 1) { throw 'Persisted payment state mismatch' }
$orders = Invoke-RestMethod "$BaseUrl/api/orders"
if (-not ($orders | Where-Object id -eq $order.id)) { throw 'Created order missing from list' }
[pscustomobject]@{
    Result = 'PASS'
    CreateOrder = $created.StatusCode
    CreatePayment = $paymentResult.StatusCode
    ReadStatus = $read.status
    PaymentCount = $read.paymentCount
    Listed = $true
    Provider = 'local simulator'
}
