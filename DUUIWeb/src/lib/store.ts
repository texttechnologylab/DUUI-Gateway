import { localStorageStore } from '@skeletonlabs/skeleton'
import { writable, type Writable } from 'svelte/store'
import { blankComponent, type DUUIComponent } from './duui/component'
import { blankPipeline, type DUUIPipeline } from './duui/pipeline'
import { blankSettings, type ProcessSettings } from './duui/process'
import type { Globals, User} from "../app";

// Stores the current state of the pipelines that is edited and used the brower's local storage.
export const currentPipelineStore: Writable<DUUIPipeline> = localStorageStore(
	'currentPipelineStore',
	blankPipeline()
)

export const examplePipelineStore: Writable<DUUIPipeline> = writable(blankPipeline())

// Stores the current state of the process settings.
export const processSettingsStore: Writable<ProcessSettings> = writable(blankSettings())

// Used in the documentation for illustration purposes.
export const exampleComponent: Writable<DUUIComponent> = writable(blankComponent('', 0))

// Store that tracks dark mode. Used for charts in both chart.ts files to check for dark mode.
export const isDarkModeStore: Writable<boolean> = writable(false)

// Store for the logged in User.
export const userSession: Writable<User> = writable(undefined)

export const defaultGlobals:Globals = {
	roles: {
		"Admin": {
			priority: 1,
			locked: true
		},
		"User": {
			priority: Infinity,
			locked: true
		}
	},
	lists: {
		"languages": {
			items: ["de", "en", "fr"]
		}
	},
	feature_restrictions: {
		"process_action_read_document": {
			restriction: 2
		}
	},
	content: {
		"how_to_cite": {
			text: "@inproceedings{Leonhardt:et:al:2023,\n\ttitle = {Unlocking the Heterogeneous Landscape of Big Data {NLP} with {DUUI}},\n\tauthor = {Leonhardt, Alexander and Abrami, Giuseppe and Baumartz, Daniel and Mehler, Alexander},\n\teditor = {Bouamor, Houda and Pino, Juan and Bali, Kalika},\n\tbooktitle = {Findings of the Association for Computational Linguistics: EMNLP 2023},\n\tyear = {2023},\n\taddress = {Singapore},\n\tpublisher = {Association for Computational Linguistics},\n\turl = {https://aclanthology.org/2023.findings-emnlp.29},\n\tpages = {385--399},\n\tpdf = {https://aclanthology.org/2023.findings-emnlp.29.pdf},\n\tabstract = {Automatic analysis of large corpora is a complex task, especially\nin terms of time efficiency. This complexity is increased by the\nfact that flexible, extensible text analysis requires the continuous\nintegration of ever new tools. Since there are no adequate frameworks\nfor these purposes in the field of NLP, and especially in the\ncontext of UIMA, that are not outdated or unusable for security\nreasons, we present a new approach to address the latter task:\nDocker Unified UIMA Interface (DUUI), a scalable, flexible, lightweight,\nand feature-rich framework for automatic distributed analysis\nof text corpora that leverages Big Data experience and virtualization\nwith Docker. We evaluate DUUI{'}s communication approach against\na state-of-the-art approach and demonstrate its outstanding behavior\nin terms of time efficiency, enabling the analysis of big text\ndata.}\n}"
		},
		"ttlab_github": {
			link: "https://github.com/texttechnologylab"
		}
	}
}

export const globalsSession: Writable<Globals> = writable(defaultGlobals)
