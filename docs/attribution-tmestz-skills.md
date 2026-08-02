# Attribution

This repository's original skills and docs are MIT-licensed (see [LICENSE](LICENSE)).

## Inspiration & recommended upstream packs

Patterns and workflows were informed by publicly documented agent-skill ecosystems. We recommend installing these upstream packs directly (do not assume they are vendored here):

| Project | License | Notes |
|---------|---------|-------|
| [addyosmani/agent-skills](https://github.com/addyosmani/agent-skills) | MIT | Lifecycle skills; five-axis review; TDD |
| [anthropics/skills](https://github.com/anthropics/skills) | Apache-2.0 / source-available (per skill) | Format examples; document skills may differ |
| [WesleySmits/agent-skills](https://github.com/WesleySmits/agent-skills) | Check repo LICENSE | Practical engineering & content skills |
| [agentskills/agentskills](https://github.com/agentskills/agentskills) | Apache-2.0 | Open Agent Skills specification |
| [JuliusBrussee/caveman](https://github.com/JuliusBrussee/caveman) | MIT | Terse "caveman" reply mode; `skills/caveman*` adapted from this idea |

`skills/caveman`, `skills/caveman-commit`, and `skills/caveman-review` are adapted under MIT from Julius Brussee's caveman project. Prefer installing the upstream pack for stats/compress/hooks: `npx skills add JuliusBrussee/caveman`.

When you copy upstream skill files into a project, keep their copyright and license notices.
