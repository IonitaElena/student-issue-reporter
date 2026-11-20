export type Complaint = {
  id: number;
  title: string;
  description: string;
  category: string;
  status: string;
  priority: string;
  userId: number;
  faculty: string | null;
  dormitory: string | null;
  building: string | null;
  locationText: string | null;
  latitude: number | null;
  longitude: number | null;
  assignedDepartment: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  voteCount: number;
  votedByMe: boolean;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type VoteSummary = { voteCount: number; votedByMe: boolean };

export const CATEGORIES = ["DORMITORY", "INFRASTRUCTURE", "ACADEMIC", "ADMINISTRATIVE", "SOCIAL", "OTHER"];

export const CATEGORY_LABEL: Record<string, string> = {
  ADMINISTRATIVE: "Administrativ",
  INFRASTRUCTURE: "Infrastructură",
  ACADEMIC: "Academic",
  SOCIAL: "Social",
  DORMITORY: "Cămin",
  OTHER: "Altele",
};

export const STATUS_LABEL: Record<string, string> = {
  NEW: "Nouă",
  IN_PROGRESS: "În lucru",
  RESOLVED: "Rezolvată",
  REJECTED: "Respinsă",
};

export const PRIORITY_LABEL: Record<string, string> = {
  LOW: "Scăzută",
  MEDIUM: "Medie",
  HIGH: "Ridicată",
  URGENT: "Urgentă",
};

export const PRIORITY_COLOR: Record<string, string> = {
  LOW: "#6b7280",
  MEDIUM: "#2563eb",
  HIGH: "#ea580c",
  URGENT: "#dc2626",
};