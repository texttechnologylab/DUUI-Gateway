type ProcessLike = {
	document_names?: string[]
	progress?: number
	status?: string
	is_finished?: boolean
}

export type ProcessTotals = {
	documentsTotal: number
	progress: number
	status?: string
	isFinished: boolean
	progressPercent: number
}

export function computeProcessTotals(
	process: ProcessLike,
	documentsTotalOverride?: number,
	paginationTotal?: number
): ProcessTotals {
	const documentsTotal =
		documentsTotalOverride ||
		process.document_names?.length ||
		paginationTotal ||
		0
	const progress = process.progress ?? 0
	const isFinished = Boolean(process.is_finished)
	const progressPercent = documentsTotal ? Math.round((progress / documentsTotal) * 100) : 0

	return {
		documentsTotal,
		progress,
		status: process.status,
		isFinished,
		progressPercent: Math.min(progressPercent, 100)
	}
}
