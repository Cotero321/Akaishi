# Generate JSON resource files for the chishi miner multiblock set
# Pure ASCII content only (PS 5.1 GBK safety)
$root = Join-Path $PSScriptRoot 'common\src\main\resources'
$blockstates = Join-Path $root 'assets\akaishi\blockstates'
$modelsBlock = Join-Path $root 'assets\akaishi\models\block'
$modelsItem  = Join-Path $root 'assets\akaishi\models\item'
$loot        = Join-Path $root 'data\akaishi\loot_tables\blocks'
$utf8NoBom   = New-Object System.Text.UTF8Encoding $false

function Write-Utf8([string]$path, [string]$content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$blocks = @(
    'akaishi_miner_controller_basic',
    'akaishi_miner_controller_advanced',
    'akaishi_miner_controller_super',
    'akaishi_miner_controller_ultimate',
    'akaishi_miner_frame',
    'akaishi_miner_upgrade_frame',
    'akaishi_miner_port'
)

foreach ($b in $blocks) {
    # blockstate: single variant -> block model
    $bs = "{`n  `"variants`": {`n    `"`": {`n      `"model`": `"akaishi:block/$b`"`n    }`n  }`n}"
    Write-Utf8 (Join-Path $blockstates ($b + '.json')) $bs

    # block model: cube_bottom_top with per-block textures
    $bm = "{`n  `"parent`": `"minecraft:block/cube_bottom_top`",`n  `"textures`": {`n    `"bottom`": `"akaishi:block/${b}_bottom`",`n    `"top`": `"akaishi:block/${b}_top`",`n    `"side`": `"akaishi:block/${b}_side`"`n  }`n}"
    Write-Utf8 (Join-Path $modelsBlock ($b + '.json')) $bm

    # item model: reuse block model
    $im = "{`n  `"parent`": `"akaishi:block/$b`"`n}"
    Write-Utf8 (Join-Path $modelsItem ($b + '.json')) $im

    # loot table: single block drop
    $lt = "{`n  `"type`": `"minecraft:block`",`n  `"pools`": [`n    {`n      `"bonus_rolls`": 0.0,`n      `"conditions`": [`n        { `"condition`": `"minecraft:survives_explosion`" }`n      ],`n      `"entries`": [`n        {`n          `"type`": `"minecraft:item`",`n          `"name`": `"akaishi:$b`"`n        }`n      ],`n      `"rolls`": 1.0`n    }`n  ]`n}"
    Write-Utf8 (Join-Path $loot ($b + '.json')) $lt
}

# upgrade item models (generated icon)
$ups = @('akaishi_miner_speed_upgrade', 'akaishi_miner_fortune_upgrade', 'akaishi_miner_storage_upgrade')
foreach ($u in $ups) {
    $um = "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": {`n    `"layer0`": `"akaishi:item/$u`"`n  }`n}"
    Write-Utf8 (Join-Path $modelsItem ($u + '.json')) $um
}

Write-Output 'miner JSON resources generated'
