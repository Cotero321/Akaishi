# Generate shaped recipes for the 4 single-slot machines + 10 miner blocks
# Pure ASCII content only (PS 5.1 GBK safety)
$recipesDir = Join-Path $PSScriptRoot 'common\src\main\resources\data\akaishi\recipes'
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Write-Utf8([string]$path, [string]$content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

function New-Shaped([string]$name, [string[]]$pattern, [hashtable]$key, [string]$result, [int]$count = 1) {
    $lines = @()
    $lines += '{'
    $lines += '  "type": "minecraft:crafting_shaped",'
    $lines += '  "pattern": ['
    foreach ($p in $pattern) { $lines += '    "' + $p + '",' }
    $lines[$lines.Count - 1] = $lines[$lines.Count - 1].TrimEnd(',')
    $lines += '  ],'
    $lines += '  "key": {'
    $entries = @()
    foreach ($k in $key.Keys) {
        $entries += '    "' + $k + '": { "item": "' + $key[$k] + '" }'
    }
    $lines += ($entries -join ",`n")
    $lines += '  },'
    $lines += '  "result": {'
    $lines += '    "item": "' + $result + '"'
    if ($count -gt 1) { $lines += '    ,"count": ' + $count }
    $lines += '  }'
    $lines += '}'
    Write-Utf8 (Join-Path $recipesDir ($name + '.json')) ($lines -join "`n")
    Write-Output ("recipe " + $name)
}

# ===== 4 single-slot machines (shared shell: iron + machine component) =====
New-Shaped 'akaishi_plant_cultivator' @('IRI','IGI','ICI') @{ I='minecraft:iron_ingot'; R='akaishi:akaishi_machine_component'; G='minecraft:glass'; C='akaishi:akaishi_crystal' } 'akaishi:akaishi_plant_cultivator'
New-Shaped 'akaishi_compressor' @('IPI','ICI','III') @{ I='minecraft:iron_ingot'; P='minecraft:piston'; C='akaishi:akaishi_machine_component' } 'akaishi:akaishi_compressor'
New-Shaped 'akaishi_pulverizer' @('IGI','IGI','ICI') @{ I='minecraft:iron_ingot'; G='minecraft:gravel'; C='akaishi:akaishi_machine_component' } 'akaishi:akaishi_pulverizer'
New-Shaped 'akaishi_transformer' @('IFI','IFI','ICI') @{ I='minecraft:iron_ingot'; F='minecraft:furnace'; C='akaishi:akaishi_machine_component' } 'akaishi:akaishi_transformer'

# ===== miner controller tiers (tier N = tier N-1 + material) =====
New-Shaped 'akaishi_miner_controller_basic' @('IGI','CRC','ICI') @{ I='minecraft:iron_ingot'; G='minecraft:gold_ingot'; R='akaishi:akaishi_machine_component'; C='akaishi:akaishi_crystal' } 'akaishi:akaishi_miner_controller_basic'
New-Shaped 'akaishi_miner_controller_advanced' @(' C ','CBC',' C ') @{ C='akaishi:akaishi_advanced_component'; B='akaishi:akaishi_miner_controller_basic' } 'akaishi:akaishi_miner_controller_advanced'
New-Shaped 'akaishi_miner_controller_super' @(' D ','DAD',' D ') @{ D='minecraft:diamond'; A='akaishi:akaishi_miner_controller_advanced' } 'akaishi:akaishi_miner_controller_super'
New-Shaped 'akaishi_miner_controller_ultimate' @(' E ','ESE',' E ') @{ E='minecraft:emerald'; S='akaishi:akaishi_miner_controller_super' } 'akaishi:akaishi_miner_controller_ultimate'

# ===== miner structural blocks =====
New-Shaped 'akaishi_miner_frame' @('ABA','B B','ABA') @{ A='minecraft:iron_ingot'; B='akaishi:akaishi_ingot' } 'akaishi:akaishi_miner_frame' 8
New-Shaped 'akaishi_miner_upgrade_frame' @('ABA','BGB','ABA') @{ A='minecraft:iron_ingot'; B='akaishi:akaishi_ingot'; G='akaishi:akaishi_machine_component' } 'akaishi:akaishi_miner_upgrade_frame' 8
New-Shaped 'akaishi_miner_port' @('III','ICI','IRI') @{ I='minecraft:iron_ingot'; C='akaishi:akaishi_crystal'; R='akaishi:akaishi_machine_component' } 'akaishi:akaishi_miner_port' 2

# ===== miner upgrade module blocks =====
New-Shaped 'akaishi_miner_speed_upgrade_block' @(' A ','ACA',' A ') @{ A='akaishi:akaishi_machine_component'; C='akaishi:akaishi_machine_speed_upgrade' } 'akaishi:akaishi_miner_speed_upgrade_block'
New-Shaped 'akaishi_miner_fortune_upgrade_block' @(' A ','ACA',' A ') @{ A='akaishi:akaishi_machine_component'; C='minecraft:emerald' } 'akaishi:akaishi_miner_fortune_upgrade_block'
New-Shaped 'akaishi_miner_storage_upgrade_block' @(' A ','ACA',' A ') @{ A='akaishi:akaishi_machine_component'; C='akaishi:akaishi_machine_energy_upgrade' } 'akaishi:akaishi_miner_storage_upgrade_block'

Write-Output 'all recipes generated'
