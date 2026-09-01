# Generate shaped recipes for the 4 single-slot machines + 10 miner blocks
# Pure ASCII content only (PS 5.1 GBK safety)
$recipesDir = Join-Path $PSScriptRoot 'common\src\main\resources\data\template_mod\recipes'
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
New-Shaped 'chishi_plant_cultivator' @('IRI','IGI','ICI') @{ I='minecraft:iron_ingot'; R='template_mod:chishi_machine_component'; G='minecraft:glass'; C='template_mod:chishi_crystal' } 'template_mod:chishi_plant_cultivator'
New-Shaped 'chishi_compressor' @('IPI','ICI','III') @{ I='minecraft:iron_ingot'; P='minecraft:piston'; C='template_mod:chishi_machine_component' } 'template_mod:chishi_compressor'
New-Shaped 'chishi_pulverizer' @('IGI','IGI','ICI') @{ I='minecraft:iron_ingot'; G='minecraft:gravel'; C='template_mod:chishi_machine_component' } 'template_mod:chishi_pulverizer'
New-Shaped 'chishi_transformer' @('IFI','IFI','ICI') @{ I='minecraft:iron_ingot'; F='minecraft:furnace'; C='template_mod:chishi_machine_component' } 'template_mod:chishi_transformer'

# ===== miner controller tiers (tier N = tier N-1 + material) =====
New-Shaped 'chishi_miner_controller_basic' @('IGI','CRC','ICI') @{ I='minecraft:iron_ingot'; G='minecraft:gold_ingot'; R='template_mod:chishi_machine_component'; C='template_mod:chishi_crystal' } 'template_mod:chishi_miner_controller_basic'
New-Shaped 'chishi_miner_controller_advanced' @(' C ','CBC',' C ') @{ C='template_mod:chishi_advanced_component'; B='template_mod:chishi_miner_controller_basic' } 'template_mod:chishi_miner_controller_advanced'
New-Shaped 'chishi_miner_controller_super' @(' D ','DAD',' D ') @{ D='minecraft:diamond'; A='template_mod:chishi_miner_controller_advanced' } 'template_mod:chishi_miner_controller_super'
New-Shaped 'chishi_miner_controller_ultimate' @(' E ','ESE',' E ') @{ E='minecraft:emerald'; S='template_mod:chishi_miner_controller_super' } 'template_mod:chishi_miner_controller_ultimate'

# ===== miner structural blocks =====
New-Shaped 'chishi_miner_frame' @('ABA','B B','ABA') @{ A='minecraft:iron_ingot'; B='template_mod:chishi_ingot' } 'template_mod:chishi_miner_frame' 8
New-Shaped 'chishi_miner_upgrade_frame' @('ABA','BGB','ABA') @{ A='minecraft:iron_ingot'; B='template_mod:chishi_ingot'; G='template_mod:chishi_machine_component' } 'template_mod:chishi_miner_upgrade_frame' 8
New-Shaped 'chishi_miner_port' @('III','ICI','IRI') @{ I='minecraft:iron_ingot'; C='template_mod:chishi_crystal'; R='template_mod:chishi_machine_component' } 'template_mod:chishi_miner_port' 2

# ===== miner upgrade module blocks =====
New-Shaped 'chishi_miner_speed_upgrade_block' @(' A ','ACA',' A ') @{ A='template_mod:chishi_machine_component'; C='template_mod:chishi_machine_speed_upgrade' } 'template_mod:chishi_miner_speed_upgrade_block'
New-Shaped 'chishi_miner_fortune_upgrade_block' @(' A ','ACA',' A ') @{ A='template_mod:chishi_machine_component'; C='minecraft:emerald' } 'template_mod:chishi_miner_fortune_upgrade_block'
New-Shaped 'chishi_miner_storage_upgrade_block' @(' A ','ACA',' A ') @{ A='template_mod:chishi_machine_component'; C='template_mod:chishi_machine_energy_upgrade' } 'template_mod:chishi_miner_storage_upgrade_block'

Write-Output 'all recipes generated'
