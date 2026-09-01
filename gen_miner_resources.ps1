# Generate JSON resource files for the chishi miner multiblock set
# Pure ASCII content only (PS 5.1 GBK safety)
$root = Join-Path $PSScriptRoot 'common\src\main\resources'
$blockstates = Join-Path $root 'assets\template_mod\blockstates'
$modelsBlock = Join-Path $root 'assets\template_mod\models\block'
$modelsItem  = Join-Path $root 'assets\template_mod\models\item'
$loot        = Join-Path $root 'data\template_mod\loot_tables\blocks'
$utf8NoBom   = New-Object System.Text.UTF8Encoding $false

function Write-Utf8([string]$path, [string]$content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$blocks = @(
    'chishi_miner_controller_basic',
    'chishi_miner_controller_advanced',
    'chishi_miner_controller_super',
    'chishi_miner_controller_ultimate',
    'chishi_miner_frame',
    'chishi_miner_upgrade_frame',
    'chishi_miner_port'
)

foreach ($b in $blocks) {
    # blockstate: single variant -> block model
    $bs = "{`n  `"variants`": {`n    `"`": {`n      `"model`": `"template_mod:block/$b`"`n    }`n  }`n}"
    Write-Utf8 (Join-Path $blockstates ($b + '.json')) $bs

    # block model: cube_bottom_top with per-block textures
    $bm = "{`n  `"parent`": `"minecraft:block/cube_bottom_top`",`n  `"textures`": {`n    `"bottom`": `"template_mod:block/${b}_bottom`",`n    `"top`": `"template_mod:block/${b}_top`",`n    `"side`": `"template_mod:block/${b}_side`"`n  }`n}"
    Write-Utf8 (Join-Path $modelsBlock ($b + '.json')) $bm

    # item model: reuse block model
    $im = "{`n  `"parent`": `"template_mod:block/$b`"`n}"
    Write-Utf8 (Join-Path $modelsItem ($b + '.json')) $im

    # loot table: single block drop
    $lt = "{`n  `"type`": `"minecraft:block`",`n  `"pools`": [`n    {`n      `"bonus_rolls`": 0.0,`n      `"conditions`": [`n        { `"condition`": `"minecraft:survives_explosion`" }`n      ],`n      `"entries`": [`n        {`n          `"type`": `"minecraft:item`",`n          `"name`": `"template_mod:$b`"`n        }`n      ],`n      `"rolls`": 1.0`n    }`n  ]`n}"
    Write-Utf8 (Join-Path $loot ($b + '.json')) $lt
}

# upgrade item models (generated icon)
$ups = @('chishi_miner_speed_upgrade', 'chishi_miner_fortune_upgrade', 'chishi_miner_storage_upgrade')
foreach ($u in $ups) {
    $um = "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": {`n    `"layer0`": `"template_mod:item/$u`"`n  }`n}"
    Write-Utf8 (Join-Path $modelsItem ($u + '.json')) $um
}

Write-Output 'miner JSON resources generated'
