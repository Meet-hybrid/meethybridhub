export type StoreConfig = {
  slug: string;
  name: string;
  shortName: string;
  descriptor: string;
  location: string;
  tagline: string;
  description: string;
  heroLabel: string;
  heroTitle: string;
  heroAccent: string;
  heroBody: string;
  categories: { name: string; description: string }[];
  navLinks: { href: string; label: string }[];
  footerShopLinks: { href: string; label: string }[];
  features: { title: string; description: string }[];
};

export const storeConfigs: Record<string, StoreConfig> = {
  "divinez-signature": {
    slug: "divinez-signature",
    name: "DivinezSignature",
    shortName: "Divinez",
    descriptor: "Handmade beadwork · Lagos",
    location: "Lagos, Nigeria",
    tagline: "Beads with a point of view.",
    description: "Hand-finished beaded accessories made to mark the everyday moments worth remembering.",
    heroLabel: "The beadwork edit · Lagos",
    heroTitle: "Made slowly.",
    heroAccent: "Worn beautifully.",
    heroBody: "Beaded bags, waist beads, necklaces, bracelets, and anklets with a warm, personal character.",
    categories: [
      { name: "Beaded Bags", description: "Small statements, beautifully carried" },
      { name: "Necklaces", description: "Layers with a little more feeling" },
      { name: "Bracelets", description: "Hand-finished everyday pieces" },
      { name: "Anklets", description: "A quiet detail for bare feet" },
      { name: "Waist Beads", description: "Color, tradition, and self-expression" },
      { name: "Sets & Gifts", description: "Pieces made to be given" },
    ],
    navLinks: [
      { href: "/", label: "Home" }, { href: "/products", label: "Shop" },
      { href: "/custom-order", label: "Custom pieces" }, { href: "/about", label: "Our story" },
    ],
    footerShopLinks: [
      { href: "/products?category=beaded%20bags", label: "Beaded bags" },
      { href: "/products?category=necklaces", label: "Necklaces" },
      { href: "/products?category=waist%20beads", label: "Waist beads" },
      { href: "/custom-order", label: "Custom pieces" },
    ],
    features: [
      { title: "Hand-finished", description: "Made with patience and a careful eye" },
      { title: "Gift-ready", description: "Thoughtful wrapping for meaningful pieces" },
      { title: "Made for you", description: "Custom colors and sizing on request" },
    ],
  },
  meethybridhub: {
    slug: "meethybridhub",
    name: "MeethybridHub",
    shortName: "Meethybrid",
    descriptor: "Clothing · Footwear · Lagos",
    location: "Lagos, Nigeria",
    tagline: "A considered way to dress.",
    description: "Clothing and footwear with a quiet point of view—selected for quality, proportion, and real life.",
    heroLabel: "The daily edit · Lagos",
    heroTitle: "A considered",
    heroAccent: "way to dress.",
    heroBody: "Clothing, shoes, and everyday pieces chosen to work hard in your wardrobe and age well.",
    categories: [
      { name: "Clothing", description: "Easy silhouettes for everyday movement" },
      { name: "Shoes", description: "Well-made footwear with a clean profile" },
      { name: "Sneakers", description: "Everyday pairs built for the city" },
      { name: "Accessories", description: "Quiet details that finish the look" },
      { name: "New Arrivals", description: "The latest pieces in the edit" },
      { name: "Essentials", description: "Reliable staples, thoughtfully selected" },
    ],
    navLinks: [
      { href: "/", label: "Home" }, { href: "/products", label: "Shop" },
      { href: "/custom-order", label: "Personal edit" }, { href: "/about", label: "About" },
    ],
    footerShopLinks: [
      { href: "/products?category=new%20arrivals", label: "New arrivals" },
      { href: "/products?category=clothing", label: "Clothing" },
      { href: "/products?category=shoes", label: "Shoes" },
      { href: "/custom-order", label: "Personal edit" },
    ],
    features: [
      { title: "Considered selection", description: "Pieces chosen for fit and longevity" },
      { title: "Reliable quality", description: "Materials that hold up to real wear" },
      { title: "Lagos delivery", description: "Clear delivery and support at every step" },
    ],
  },
};

const configuredSlug = process.env.NEXT_PUBLIC_STORE_SLUG || "meethybridhub";

export function getStoreConfigFromHost(host = ""): StoreConfig {
  const hostname = host.split(":")[0].toLowerCase();
  if (hostname.includes("divinezsignature") || hostname.includes("divinez-signature")) {
    return storeConfigs["divinez-signature"];
  }
  if (hostname.includes("meethybridhub")) return storeConfigs.meethybridhub;
  return getStoreConfig(configuredSlug);
}

export function getStoreConfig(slug = configuredSlug): StoreConfig {
  return storeConfigs[slug] ?? storeConfigs.meethybridhub;
}

export const activeStore = getStoreConfig();
