export type StoreConfig = {
  slug: string;
  name: string;
  shortName: string;
  parentBrand: string;
  subBrand: string;
  subBrandDescriptor: string;
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
    parentBrand: "DivinezSignature",
    subBrand: "Beads by Divine",
    subBrandDescriptor: "A DivinezSignature subsidiary",
    descriptor: "Handcrafted Beadwork · Made in Abuja",
    location: "Abuja, Nigeria",
    tagline: "Beads with a point of view.",
    description: "Handcrafted luxury beaded accessories designed to earn its place in your collection.",
    heroLabel: "Handcrafted Beadwork · Made in Abuja",
    heroTitle: "Handcrafted luxury, designed to earn",
    heroAccent: "its place in your collection.",
    heroBody: "Explore bespoke crystal charm bracelets, beaded waist chains, and custom artisan jewelry tailored for modern elegance.",
    categories: [
      { name: "Bags", description: "Beaded evening bags and clutches" },
      { name: "Anklets", description: "Crystal and beaded ankle chains" },
      { name: "Bracelets", description: "Crystal charm bracelets and wrist beads" },
      { name: "Waist Beads", description: "Traditional and modern waist chains" },
      { name: "Necklaces", description: "Layered necklaces and beaded pendants" },
      { name: "Sets & Gifts", description: "Curated gift sets and collections" },
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
      { title: "Handcrafted in Abuja", description: "Every piece made with care in our studio" },
      { title: "Gift-ready packaging", description: "Beautifully wrapped for meaningful moments" },
      { title: "Custom orders welcome", description: "Bespoke colors, sizes, and designs available" },
    ],
  },
  meethybridhub: {
    slug: "meethybridhub",
    name: "MeethybridHub",
    shortName: "Meethybrid",
    parentBrand: "MeethybridHub",
    subBrand: "MeethybridHub",
    subBrandDescriptor: "The daily edit",
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
